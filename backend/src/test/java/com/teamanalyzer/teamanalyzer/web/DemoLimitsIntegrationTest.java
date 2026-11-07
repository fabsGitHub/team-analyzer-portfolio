package com.teamanalyzer.teamanalyzer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Limits for anonymous demo visitors: what one demo token may create in its workspace, how many
 * workspaces may be started and exist, and a per-address limit that cannot lock out everyone.
 * Small limits keep the tests short; every test starts without any demo workspace.
 */
@SpringBootTest(properties = {
        "app.demo.enabled=true",
        "app.demo.max-sessions-per-address-per-hour=2",
        "app.demo.tracked-addresses=2",
        "app.demo.max-sessions-per-hour=6",
        "app.demo.max-live-workspaces=10",
        "app.demo.max-surveys-per-workspace=2",
        "app.demo.max-tokens-per-survey=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoLimitsIntegrationTest {

    private static final String DEMO_EMAILS = "demo+%@example.invalid";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void removeAllDemoWorkspaces() {
        jdbc.update("""
                delete from teams where id in (
                  select team_id from (
                    select tm.team_id from team_members tm join users u on u.id = tm.user_id
                     where u.email like ?) demo_teams)""", DEMO_EMAILS);
        jdbc.update("delete from users where email like ?", DEMO_EMAILS);
    }

    // --- what one workspace may hold ---

    @Test
    void a_demo_workspace_holds_a_limited_number_of_surveys() throws Exception {
        Session demo = startDemo(newAddress());

        // The seeded survey is the first of two.
        createSurvey(demo, "Second survey").andExpect(status().isCreated());
        createSurvey(demo, "Third survey")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo.limit.surveys"))
                .andExpect(jsonPath("$.limit").value(2));

        assertThat(surveysOf(demo)).isEqualTo(2);
    }

    @Test
    void concurrent_requests_cannot_overrun_the_survey_limit() throws Exception {
        Session demo = startDemo(newAddress());

        List<Integer> statuses = inParallel(8, () -> createSurvey(demo, "Burst").andReturn().getResponse().getStatus());

        assertThat(statuses).containsOnly(201, 403);
        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(surveysOf(demo)).isEqualTo(2);
    }

    @Test
    void a_demo_survey_hands_out_a_limited_number_of_participation_links() throws Exception {
        Session demo = startDemo(newAddress());

        mvc.perform(put("/api/surveys/{id}/my-token", demo.surveyId()).header("Authorization", demo.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(true));
        // Renewing revokes the open link and issues a new one, which counts as well.
        mvc.perform(post("/api/surveys/{id}/my-token/renew", demo.surveyId()).header("Authorization", demo.bearer()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/surveys/{id}/my-token/renew", demo.surveyId()).header("Authorization", demo.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo.limit.tokens"))
                .andExpect(jsonPath("$.limit").value(2));

        assertThat(jdbc.queryForObject("select count(*) from survey_tokens where survey_id = uuid_to_bin(?)",
                Integer.class, demo.surveyId())).isEqualTo(2);
        // The rejected renewal was rolled back: the second link is still open.
        mvc.perform(put("/api/surveys/{id}/my-token", demo.surveyId()).header("Authorization", demo.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(false));
    }

    @Test
    void refreshing_a_demo_session_keeps_one_refresh_token_row() throws Exception {
        Session demo = startDemo(newAddress());
        Cookie cookie = demo.refreshCookie();

        for (int i = 0; i < 3; i++) {
            cookie = mvc.perform(post("/api/auth/refresh").cookie(cookie))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getCookie("refresh_token");
        }

        assertThat(jdbc.queryForObject("""
                select count(*) from refresh_tokens rt join users u on u.id = rt.user_id
                 where u.email like ?""", Integer.class, DEMO_EMAILS)).isEqualTo(1);
    }

    @Test
    void survey_titles_and_questions_are_limited_to_the_column_lengths() throws Exception {
        Session demo = startDemo(newAddress());

        createSurvey(demo, "t".repeat(301), "q").andExpect(status().isBadRequest());
        createSurvey(demo, "Title", "q".repeat(1001)).andExpect(status().isBadRequest());
        assertThat(surveysOf(demo)).isEqualTo(1);
    }

    @Test
    void demo_addresses_cannot_be_registered() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "email", "demo+squatter@example.invalid", "password", "correct horse battery"))))
                .andExpect(status().isBadRequest());
        assertThat(demoAccounts()).isZero();
    }

    // --- how many workspaces may be started and exist ---

    @Test
    void demo_starts_are_capped_per_hour_across_all_addresses() throws Exception {
        insertDemoAccounts(6, 0); // six starts in the last hour, e.g. on other instances

        mvc.perform(post("/api/demo/session").header("X-Real-IP", newAddress()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "3600"))
                .andExpect(jsonPath("$.code").value("demo.limit.hourly"))
                .andExpect(jsonPath("$.limit").value(6));

        jdbc.update("update users set created_at = now() - interval 2 hour where email like ?", DEMO_EMAILS);
        startDemo(newAddress());
    }

    @Test
    void demo_starts_stop_while_all_workspaces_are_in_use() throws Exception {
        insertDemoAccounts(9, 2);
        startDemo(newAddress()); // the tenth

        mvc.perform(post("/api/demo/session").header("X-Real-IP", newAddress()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("demo.limit.capacity"))
                .andExpect(jsonPath("$.limit").value(10));

        // Once workspaces expire, the next start deletes them and goes through.
        jdbc.update("update users set created_at = now() - interval 25 hour where email like ?", DEMO_EMAILS);
        startDemo(newAddress());
        assertThat(demoAccounts()).isEqualTo(1);
    }

    @Test
    void the_address_limit_never_locks_out_new_visitors() throws Exception {
        String busy = newAddress();
        startDemo(busy);
        startDemo(busy);
        mvc.perform(post("/api/demo/session").header("X-Real-IP", busy))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("demo.limit.address"))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));

        // Only two addresses are tracked. Before, every address beyond the tracked ones shared one
        // bucket with the per-address limit, so the fourth of these new visitors was turned away.
        for (int i = 0; i < 4; i++) {
            startDemo(newAddress());
        }
        assertThat(demoAccounts()).isEqualTo(6);
    }

    // --- helpers ---

    private record Session(String accessToken, String surveyId, String teamId, Cookie refreshCookie) {
        String bearer() {
            return "Bearer " + accessToken;
        }
    }

    private static int addressCounter;

    private static String newAddress() {
        addressCounter++;
        return "198.18." + (addressCounter / 250) + "." + (addressCounter % 250 + 1);
    }

    private Session startDemo(String address) throws Exception {
        MvcResult started = mvc.perform(post("/api/demo/session").header("X-Real-IP", address))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(started.getResponse().getContentAsString());
        String surveyId = body.get("surveyId").asString();
        String teamId = jdbc.queryForObject("select bin_to_uuid(team_id) from surveys where id = uuid_to_bin(?)",
                String.class, surveyId);
        return new Session(body.get("accessToken").asString(), surveyId, teamId,
                started.getResponse().getCookie("refresh_token"));
    }

    private ResultActions createSurvey(Session demo, String title) throws Exception {
        return createSurvey(demo, title, "How are we doing?");
    }

    private ResultActions createSurvey(Session demo, String title, String question) throws Exception {
        return mvc.perform(post("/api/surveys")
                .header("Authorization", demo.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "teamId", demo.teamId(),
                        "title", title,
                        "questions", List.of(question, "Q2", "Q3", "Q4", "Q5")))));
    }

    private int surveysOf(Session demo) {
        return jdbc.queryForObject("select count(*) from surveys where team_id = uuid_to_bin(?)",
                Integer.class, demo.teamId());
    }

    private int demoAccounts() {
        return jdbc.queryForObject("select count(*) from users where email like ?", Integer.class, DEMO_EMAILS);
    }

    private void insertDemoAccounts(int count, int hoursAgo) {
        for (int i = 0; i < count; i++) {
            jdbc.update("""
                    insert into users (id, email, password_hash, enabled, created_at)
                    values (uuid_to_bin(uuid()), concat('demo+', uuid(), '@example.invalid'), 'h', true,
                            now() - interval ? hour)""", hoursAgo);
        }
    }

    private static <T> List<T> inParallel(int requests, Callable<T> request) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(pool.submit(request));
            }
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}

package com.teamanalyzer.teamanalyzer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamanalyzer.teamanalyzer.service.DemoWorkspaceService;

/**
 * "Try the live demo": a private, seeded workspace per click, signed in for 24 hours and
 * deleted (with its answers) once it has expired.
 */
@SpringBootTest(properties = "app.demo.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoSessionIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    DemoWorkspaceService demo;

    @Test
    void a_demo_session_opens_a_seeded_workspace_for_24_hours() throws Exception {
        MvcResult started = startDemo();
        JsonNode body = json.readTree(started.getResponse().getContentAsString());
        String surveyId = body.get("surveyId").asText();

        assertThat(started.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=86400");

        MvcResult results = mvc.perform(get("/api/surveys/{id}/results", surveyId)
                .header("Authorization", "Bearer " + body.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json.readTree(results.getResponse().getContentAsString()).get("n").asInt()).isEqualTo(18);

        // Seeded responses are stored like real ones: with the submission date only.
        List<Long> secondsOfDay = jdbc.queryForList("""
                select unix_timestamp(created_at) % 86400 from survey_responses
                 where survey_id = uuid_to_bin(?)""", Long.class, surveyId);
        assertThat(secondsOfDay).hasSize(18).containsOnly(0L);

        // A refreshed demo session must not outlive the workspace.
        Cookie refresh = started.getResponse().getCookie("refresh_token");
        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(refreshed.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=86400");
    }

    @Test
    void expired_workspaces_are_deleted_with_their_answers() throws Exception {
        Workspace expired = workspace(startDemo());
        Workspace fresh = workspace(startDemo());
        age(expired);

        demo.cleanupExpiredWorkspaces();

        assertThat(count("select count(*) from users where id = uuid_to_bin(?)", expired.userId())).isZero();
        assertThat(count("select count(*) from teams where id = uuid_to_bin(?)", expired.teamId())).isZero();
        assertThat(count("select count(*) from survey_responses where survey_id = uuid_to_bin(?)", expired.surveyId()))
                .isZero();
        assertThat(count("select count(*) from survey_responses where survey_id = uuid_to_bin(?)", fresh.surveyId()))
                .isEqualTo(18);
    }

    @Test
    void cleanup_keeps_a_team_that_has_a_real_member() throws Exception {
        Workspace shared = workspace(startDemo());
        String realUser = UUID.randomUUID().toString();
        jdbc.update("insert into users (id, email, password_hash, enabled) values (uuid_to_bin(?), ?, 'h', true)",
                realUser, "real-" + realUser + "@example.test");
        jdbc.update("insert into team_members (team_id, user_id, leader) values (uuid_to_bin(?), uuid_to_bin(?), false)",
                shared.teamId(), realUser);
        age(shared);

        demo.cleanupExpiredWorkspaces();

        assertThat(count("select count(*) from teams where id = uuid_to_bin(?)", shared.teamId())).isEqualTo(1);
        assertThat(count("select count(*) from survey_responses where survey_id = uuid_to_bin(?)", shared.surveyId()))
                .isEqualTo(18);
        // Its demo account still owns the survey, so it stays as well.
        assertThat(count("select count(*) from users where id = uuid_to_bin(?)", shared.userId())).isEqualTo(1);
    }

    @Test
    void the_rate_limit_ignores_client_supplied_forwarded_for_entries() throws Exception {
        String address = "203.0.113." + (System.nanoTime() % 250 + 1);
        for (int i = 0; i < 20; i++) {
            mvc.perform(post("/api/demo/session")
                    .header("X-Real-IP", address)
                    .header("X-Forwarded-For", "192.0.2." + i + ", " + address))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/demo/session")
                .header("X-Real-IP", address)
                .header("X-Forwarded-For", "192.0.2.99, " + address))
                .andExpect(status().isTooManyRequests());
    }

    // --- helpers ---

    private record Workspace(String userId, String teamId, String surveyId) {
    }

    private MvcResult startDemo() throws Exception {
        // A distinct client address per call keeps the per-IP limit out of these tests.
        return mvc.perform(post("/api/demo/session")
                .header("X-Real-IP", "198.51.100." + (System.nanoTime() % 250 + 1)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private Workspace workspace(MvcResult started) throws Exception {
        String surveyId = json.readTree(started.getResponse().getContentAsString()).get("surveyId").asText();
        Map<String, Object> row = jdbc.queryForMap(
                "select bin_to_uuid(created_by) as user_id, bin_to_uuid(team_id) as team_id from surveys where id = uuid_to_bin(?)",
                surveyId);
        return new Workspace((String) row.get("user_id"), (String) row.get("team_id"), surveyId);
    }

    private void age(Workspace workspace) {
        jdbc.update("update users set created_at = now() - interval 25 hour where id = uuid_to_bin(?)",
                workspace.userId());
    }

    private int count(String sql, String id) {
        return jdbc.queryForObject(sql, Integer.class, id);
    }
}

package com.teamanalyzer.teamanalyzer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamanalyzer.teamanalyzer.port.EmailSender;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;

import jakarta.servlet.http.Cookie;

/**
 * End-to-end authentication flow against a real MySQL schema (Testcontainers + Flyway).
 * Outgoing mail is captured so verification and reset links can be followed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9._\\-]+)");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    UserRepository users;

    @MockitoBean
    EmailSender emailSender;

    @Test
    void register_verify_login_refresh_logout_and_reset_password() throws Exception {
        String email = "flow-" + System.nanoTime() + "@example.test";
        String password = "correct horse battery";

        // Register -> verification mail is sent, account stays disabled.
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email, "password", password)))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email, "password", password)))
                .andExpect(status().isUnauthorized());

        // Verify via the emailed link.
        String verifyToken = tokenFromLastMail(email, "Confirm");
        mvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                .content(body("token", verifyToken, "password", password)))
                .andExpect(status().isNoContent());

        // Login -> access token + rotating refresh cookie.
        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email, "password", password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String access = json.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
        Cookie refresh = login.getResponse().getCookie("refresh_token");
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andReturn();
        Cookie rotated = refreshed.getResponse().getCookie("refresh_token");
        // The old refresh token is single-use.
        mvc.perform(post("/api/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());

        // Password reset: token is stored hashed, works once, and ends existing sessions.
        mvc.perform(post("/api/auth/reset").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email)))
                .andExpect(status().isOk());
        String resetToken = tokenFromLastMail(email, "Reset");
        assertThat(users.findByEmail(email).orElseThrow().getResetToken())
                .isNotBlank()
                .isNotEqualTo(resetToken);

        String newPassword = "another long passphrase";
        mvc.perform(post("/api/auth/reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(body("token", resetToken, "newPassword", newPassword)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(body("token", resetToken, "newPassword", newPassword)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/refresh").cookie(rotated)).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email, "password", password)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email, "password", newPassword)))
                .andExpect(status().isOk());
    }

    @Test
    void logout_without_cookie_is_a_no_op() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
    }

    @Test
    void reset_request_for_unknown_address_does_not_reveal_anything() throws Exception {
        mvc.perform(post("/api/auth/reset").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", "nobody@example.test")))
                .andExpect(status().isOk());
    }

    @Test
    void unknown_endpoints_are_404_not_server_errors() throws Exception {
        mvc.perform(post("/api/auth/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        // The demo endpoint is public but absent while demo mode is off (the default), so the
        // landing page can tell "not enabled" apart from "service unavailable".
        mvc.perform(post("/api/demo/session"))
                .andExpect(status().isNotFound());
    }

    @Test
    void health_endpoint_is_public() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void confirmation_requires_the_password_so_a_stranger_registered_first_cannot_win() throws Exception {
        String email = "first-" + System.nanoTime() + "@example.test";
        register(email, "stranger chosen password");
        register(email, "owner chosen password"); // unconfirmed address: owner may register again

        String link = tokensFromMails(email, "Confirm").get(0); // the mail the stranger triggered
        confirm(link, "stranger chosen password", 400);          // stranger's password no longer valid
        confirm(link, "owner chosen password", 204);             // owner confirms with their own password

        login(email, "stranger chosen password", 401);
        login(email, "owner chosen password", 200);
        register(email, "stranger chosen password", 409);       // confirmed addresses are taken
    }

    @Test
    void a_stranger_registering_after_the_owner_cannot_take_over_the_account() throws Exception {
        String email = "second-" + System.nanoTime() + "@example.test";
        register(email, "owner chosen password");
        register(email, "stranger chosen password"); // replaces the pending password

        // Every confirmation link now needs the stranger's password, which the mailbox owner
        // does not know, and the stranger does not have the links.
        for (String link : tokensFromMails(email, "Confirm")) {
            confirm(link, "owner chosen password", 400);
        }
        login(email, "stranger chosen password", 401);

        // The owner recovers by registering again (or by resetting the password).
        register(email, "owner chosen password");
        List<String> links = tokensFromMails(email, "Confirm");
        confirm(links.get(links.size() - 1), "owner chosen password", 204);
        login(email, "owner chosen password", 200);
        login(email, "stranger chosen password", 401);
    }

    @Test
    void completing_a_password_reset_confirms_the_account() throws Exception {
        String email = "resetfirst-" + System.nanoTime() + "@example.test";
        register(email, "forgotten password 1");
        mvc.perform(post("/api/auth/reset").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email)))
                .andExpect(status().isOk());
        String resetToken = tokenFromLastMail(email, "Reset");
        mvc.perform(post("/api/auth/reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(body("token", resetToken, "newPassword", "a brand new password")))
                .andExpect(status().isOk());

        login(email, "a brand new password", 200);
    }

    @Test
    void reset_reaches_exactly_the_requested_account_even_with_case_folding_twins() throws Exception {
        String n = Long.toString(System.nanoTime());
        String real = "ivy-" + n + "@example.test";
        String twin = "\u0131vy-" + n + "@example.test"; // dotless i: upper() would merge it with "ivy"
        register(real, "the real password");
        confirm(tokensFromMails(real, "Confirm").get(0), "the real password", 204);
        register(twin, "the twin password");

        mvc.perform(post("/api/auth/reset").contentType(MediaType.APPLICATION_JSON).content(body("email", real)))
                .andExpect(status().isOk());
        assertThat(tokensFromMails(real, "Reset")).hasSize(1);
        org.mockito.Mockito.verify(emailSender, org.mockito.Mockito.never())
                .send(eq(twin), startsWith("Reset"), anyString());
        login(real, "the real password", 200);
    }

    @Test
    void concurrent_registrations_of_one_address_never_fail() throws Exception {
        String email = "race-" + System.nanoTime() + "@example.test";
        var pool = java.util.concurrent.Executors.newFixedThreadPool(6);
        try {
            var start = new java.util.concurrent.CountDownLatch(1);
            List<java.util.concurrent.Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                String password = "racing password " + i;
                results.add(pool.submit(() -> {
                    start.await();
                    return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(body("email", email, "password", password)))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (var r : results) {
                assertThat(r.get()).isEqualTo(202);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(users.findByEmail(email)).isPresent();
    }

    @Test
    void addresses_with_invisible_characters_are_rejected() throws Exception {
        register("in\u200bvisible-" + System.nanoTime() + "@example.test", "a long enough password", 400);
    }

    private void register(String email, String password) throws Exception {
        register(email, password, 202);
    }

    private void register(String email, String password, int expectedStatus) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email, "password", password)))
                .andExpect(status().is(expectedStatus));
    }

    private void confirm(String token, String password, int expectedStatus) throws Exception {
        mvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                .content(body("token", token, "password", password)))
                .andExpect(status().is(expectedStatus));
    }

    private void login(String email, String password, int expectedStatus) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email, "password", password)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void failed_login_explains_itself_without_leaking_details() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", "nobody-" + System.nanoTime() + "@example.test", "password", "whatever123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Invalid email or password")));
    }

    @Test
    void reset_request_answers_200_even_when_mail_delivery_fails() throws Exception {
        String email = "mailfail-" + System.nanoTime() + "@example.test";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email, "password", "a long enough password")))
                .andExpect(status().isAccepted());
        doThrow(new MailSendException("provider down")).when(emailSender)
                .send(eq(email), startsWith("Reset"), anyString());

        mvc.perform(post("/api/auth/reset").contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email)))
                .andExpect(status().isOk());
    }

    @Test
    void cors_allows_only_configured_origins() throws Exception {
        mvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/auth/login")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void same_origin_requests_through_a_proxy_are_not_treated_as_cors() throws Exception {
        // nginx forwards Host with port (proxy_set_header Host $http_host): an unlisted origin
        // that matches the request's own host must reach the controller, not be rejected.
        mvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setServerName("127.0.0.1");
                    request.setServerPort(5173);
                    return request;
                })
                .header("Origin", "http://127.0.0.1:5173")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("email", "nobody@example.test", "password", "whatever123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void health_probe_groups_are_public() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void outgoing_http_calls_time_out(@Autowired RestClient.Builder builder) throws Exception {
        // A server that accepts connections but never answers, like a stalled mail API.
        try (ServerSocket silent = new ServerSocket(0)) {
            RestClient client = builder.baseUrl("http://127.0.0.1:" + silent.getLocalPort()).build();
            long start = System.nanoTime();
            assertThatThrownBy(() -> client.get().uri("/").retrieve().toBodilessEntity())
                    .isInstanceOf(ResourceAccessException.class);
            long seconds = Duration.ofNanos(System.nanoTime() - start).toSeconds();
            assertThat(seconds).isBetween(5L, 30L);
        }
    }

    private List<String> tokensFromMails(String to, String subjectPrefix) {
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).send(eq(to), subject.capture(), text.capture());
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < subject.getAllValues().size(); i++) {
            if (subject.getAllValues().get(i).startsWith(subjectPrefix)) {
                Matcher m = TOKEN.matcher(text.getAllValues().get(i));
                assertThat(m.find()).as("mail contains a token link").isTrue();
                tokens.add(m.group(1));
            }
        }
        return tokens;
    }

    private String tokenFromLastMail(String to, String subjectPrefix) {
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).send(eq(to), subject.capture(), text.capture());
        for (int i = subject.getAllValues().size() - 1; i >= 0; i--) {
            if (subject.getAllValues().get(i).startsWith(subjectPrefix)) {
                Matcher m = TOKEN.matcher(text.getAllValues().get(i));
                assertThat(m.find()).as("mail contains a token link").isTrue();
                return m.group(1);
            }
        }
        throw new AssertionError("No mail starting with '" + subjectPrefix + "' sent to " + to);
    }

    private String body(String... keyValues) throws Exception {
        var node = json.createObjectNode();
        for (int i = 0; i < keyValues.length; i += 2) {
            node.put(keyValues[i], keyValues[i + 1]);
        }
        return json.writeValueAsString(node);
    }
}

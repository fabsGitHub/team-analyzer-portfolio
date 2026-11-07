package com.teamanalyzer.teamanalyzer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.teamanalyzer.teamanalyzer.domain.DemoAccounts;
import com.teamanalyzer.teamanalyzer.domain.RefreshToken;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.repo.RefreshTokenRepository;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import com.teamanalyzer.teamanalyzer.service.DemoWorkspaceCleanup;
import com.teamanalyzer.teamanalyzer.service.JwtService;

/**
 * Switching demo mode off ends the demo sessions that already exist: their access and refresh
 * tokens stop working, and their workspaces are still deleted once they expire.
 */
@SpringBootTest(properties = "app.demo.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoModeOffIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UserRepository users;
    @Autowired
    RefreshTokenRepository refreshTokens;
    @Autowired
    JwtService jwt;
    @Autowired
    DemoWorkspaceCleanup cleanup;

    @Test
    void demo_access_tokens_no_longer_sign_in() throws Exception {
        User demo = workspace(DemoAccounts.emailFor(UUID.randomUUID()));
        User real = workspace("real-" + UUID.randomUUID() + "@example.test");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + jwt.createAccessToken(demo)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + jwt.createAccessToken(real)))
                .andExpect(status().isOk());
    }

    @Test
    void demo_refresh_tokens_are_refused_and_revoked() throws Exception {
        User demo = workspace(DemoAccounts.emailFor(UUID.randomUUID()));
        String plain = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken token = refreshTokens.save(RefreshToken.create(
                demo, sha256Base64Url(plain), Instant.now().plus(24, ChronoUnit.HOURS), "test", "127.0.0.1"));

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", plain)))
                .andExpect(status().isUnauthorized());

        assertThat(refreshTokens.findById(token.getId())).get().matches(RefreshToken::isRevoked);
    }

    @Test
    void expired_demo_workspaces_are_still_deleted() {
        User demo = workspace(DemoAccounts.emailFor(UUID.randomUUID()));
        String teamId = jdbc.queryForObject(
                "select bin_to_uuid(team_id) from team_members where user_id = uuid_to_bin(?)",
                String.class, demo.getId().toString());
        jdbc.update("update users set created_at = now() - interval 25 hour where id = uuid_to_bin(?)",
                demo.getId().toString());

        cleanup.cleanupExpiredWorkspaces();

        assertThat(users.existsById(demo.getId())).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from teams where id = uuid_to_bin(?)", Integer.class, teamId))
                .isZero();
    }

    // --- helpers ---

    /** An enabled LEADER account with its own team, as a demo start would have created it. */
    private User workspace(String email) {
        String userId = UUID.randomUUID().toString();
        String teamId = UUID.randomUUID().toString();
        jdbc.update("insert into users (id, email, password_hash, enabled) values (uuid_to_bin(?), ?, 'h', true)",
                userId, email);
        jdbc.update("insert into user_roles (user_id, role) values (uuid_to_bin(?), 'LEADER')", userId);
        jdbc.update("insert into teams (id, name) values (uuid_to_bin(?), 'Team')", teamId);
        jdbc.update("insert into team_members (team_id, user_id, leader) values (uuid_to_bin(?), uuid_to_bin(?), true)",
                teamId, userId);
        return users.findByEmailWithRoles(email).orElseThrow();
    }

    private static String sha256Base64Url(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}

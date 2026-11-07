package com.teamanalyzer.teamanalyzer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
import com.teamanalyzer.teamanalyzer.port.EmailSender;

/**
 * APP_BOOTSTRAP_ADMIN_EMAIL: the configured address becomes ADMIN on confirmation, which needs
 * the mailbox link and the password. A stranger who registers the address (before or after the
 * owner) can never get the role, because they lack the link and the owner lacks their password.
 */
@SpringBootTest(properties = "app.auth.bootstrap-admin-email=" + BootstrapAdminIntegrationTest.OWNER)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BootstrapAdminIntegrationTest {

    static final String OWNER = "bootstrap-owner@example.test";
    /** Matches OWNER under Java's equalsIgnoreCase (long s folds to s), but is another mailbox. */
    static final String LONG_S_TWIN = "boot\u017ftrap-owner@example.test";
    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9._\\-]+)");

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @MockitoBean
    EmailSender emailSender;
    @Autowired
    JdbcTemplate jdbc;

    /** Every test starts without the bootstrap address (and its look-alike) registered. */
    @BeforeEach
    void forgetBootstrapAccounts() {
        jdbc.update("delete from users where email in (?, ?, ?)", OWNER, OWNER.replace("example", "ex\u00e4mple"),
                LONG_S_TWIN);
    }

    @Test
    void a_stranger_who_registers_the_bootstrap_address_never_becomes_admin() throws Exception {
        // Owner registers, a stranger registers the same (still unconfirmed) address afterwards.
        register("owner password 4567");
        register("stranger password 123");

        // The owner clicks every confirmation mail with their own password: none activates the
        // stranger's password, so nobody becomes ADMIN with it.
        for (String link : confirmationTokens()) {
            confirm(link, "owner password 4567", 400);
        }
        login("stranger password 123", 401);

        // The owner takes the address back and confirms it: ADMIN goes to the owner's password.
        register("owner password 4567");
        List<String> links = confirmationTokens();
        confirm(links.get(links.size() - 1), "owner password 4567", 204);
        login("stranger password 123", 401);
        String access = json.readTree(login("owner password 4567", 200)).get("accessToken").asString();

        String me = mvc.perform(get("/api/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(me).get("roles").toString()).contains("ADMIN");
    }

    @Test
    void a_look_alike_address_is_a_different_account() throws Exception {
        // MySQL's default collation is accent-insensitive; with it, this address matched the
        // owner's row, overwrote its pending password and received a confirmation link.
        String lookAlike = OWNER.replace("example", "ex\u00e4mple");
        register("owner password 4567");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("email", lookAlike, "password", "stranger password 123"))))
                .andExpect(status().isAccepted());

        // The look-alike's confirmation goes to the look-alike and confirms only that account.
        ArgumentCaptor<String> strangerMail = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(lookAlike), org.mockito.ArgumentMatchers.startsWith("Confirm"), strangerMail.capture());
        Matcher m = TOKEN.matcher(strangerMail.getValue());
        assertThat(m.find()).isTrue();
        confirm(m.group(1), "stranger password 123", 204);
        login("stranger password 123", 401); // not the owner's account

        // The owner's pending registration is untouched and becomes the administrator.
        List<String> links = confirmationTokens();
        confirm(links.get(links.size() - 1), "owner password 4567", 204);
        String access = json.readTree(login("owner password 4567", 200)).get("accessToken").asString();
        String me = mvc.perform(get("/api/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(me).get("email").asString()).isEqualTo(OWNER);
        assertThat(json.readTree(me).get("roles").toString()).contains("ADMIN");
    }

    @Test
    void a_case_folding_twin_of_the_bootstrap_address_does_not_become_admin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("email", LONG_S_TWIN, "password", "twin password 1234"))))
                .andExpect(status().isAccepted());
        ArgumentCaptor<String> mail = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(LONG_S_TWIN), org.mockito.ArgumentMatchers.startsWith("Confirm"), mail.capture());
        Matcher m = TOKEN.matcher(mail.getValue());
        assertThat(m.find()).isTrue();
        confirm(m.group(1), "twin password 1234", 204);

        String access = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("email", LONG_S_TWIN, "password", "twin password 1234"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asString();
        String me = mvc.perform(get("/api/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(me).get("roles").toString()).doesNotContain("ADMIN");
    }

    private void confirm(String token, String password, int expected) throws Exception {
        mvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("token", token, "password", password))))
                .andExpect(status().is(expected));
    }

    private String login(String password, int expected) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(password)))
                .andExpect(status().is(expected))
                .andReturn().getResponse().getContentAsString();
    }

    private void register(String password) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(password)))
                .andExpect(status().isAccepted());
    }

    private String credentials(String password) throws Exception {
        return json.writeValueAsString(java.util.Map.of("email", OWNER, "password", password));
    }

    private List<String> confirmationTokens() {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).send(eq(OWNER), org.mockito.ArgumentMatchers.startsWith("Confirm"), text.capture());
        List<String> tokens = new ArrayList<>();
        for (String t : text.getAllValues()) {
            Matcher m = TOKEN.matcher(t);
            if (m.find()) tokens.add(m.group(1));
        }
        assertThat(tokens).isNotEmpty();
        return tokens;
    }
}

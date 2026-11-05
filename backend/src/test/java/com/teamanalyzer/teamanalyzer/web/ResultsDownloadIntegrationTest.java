package com.teamanalyzer.teamanalyzer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamanalyzer.teamanalyzer.domain.Role;
import com.teamanalyzer.teamanalyzer.domain.Survey;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import com.teamanalyzer.teamanalyzer.service.SurveyService;
import com.teamanalyzer.teamanalyzer.service.TeamService;

/**
 * The JSON download link is opened by the browser without an Authorization header, so access
 * is decided for the user the signed link was issued to: the team's leaders and administrators.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResultsDownloadIntegrationTest {

    private static final String PASSWORD = "download test password";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    UserRepository users;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    TeamService teams;
    @Autowired
    SurveyService surveys;
    @Autowired
    com.teamanalyzer.teamanalyzer.service.TokenService tokens;
    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void leaders_and_admins_can_download_members_cannot() throws Exception {
        String run = Long.toString(System.nanoTime());
        User leader = user("lead-" + run, false);
        User member = user("member-" + run, false);
        User admin = user("admin-" + run, true); // not part of the team

        var team = teams.createTeam("Team " + run, leader.getId());
        teams.addMember(team.getId(), member.getId(), false);
        Survey survey = surveys.createSurvey(leader.getId(), team.getId(), "Pulse " + run,
                List.of("Q1", "Q2", "Q3", "Q4", "Q5"));

        assertThat(download(survey, leader)).isEqualTo(200);
        assertThat(download(survey, admin)).isEqualTo(200);
        mvc.perform(post("/api/surveys/{id}/download-tokens", survey.getId())
                .header("Authorization", "Bearer " + login(member)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admins_can_delete_a_team_whose_survey_has_answers() throws Exception {
        String run = Long.toString(System.nanoTime());
        User leader = user("del-lead-" + run, false);
        User admin = user("del-admin-" + run, true);
        var team = teams.createTeam("Delete me " + run, leader.getId());
        Survey survey = surveys.createSurvey(leader.getId(), team.getId(), "Pulse " + run,
                List.of("Q1", "Q2", "Q3", "Q4", "Q5"));
        String token = tokens.ensurePersonalToken(survey, leader.getId(), leader.getEmail());
        mvc.perform(post("/api/surveys/{id}/responses", survey.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"q1\":5,\"q2\":4,\"q3\":3,\"q4\":2,\"q5\":1}"))
                .andExpect(status().isAccepted());

        mvc.perform(delete("/api/admin/teams/{id}", team.getId()).header("Authorization", "Bearer " + login(admin)))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from surveys where id = uuid_to_bin(?)", Integer.class,
                survey.getId().toString())).isZero();
    }

    /** Issues a link as the given user, then opens it anonymously like the browser does. */
    private int download(Survey survey, User user) throws Exception {
        String body = mvc.perform(post("/api/surveys/{id}/download-tokens", survey.getId())
                .header("Authorization", "Bearer " + login(user)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        URI url = URI.create(json.readTree(body).get("url").asText());
        return mvc.perform(get(url.getPath() + "?" + url.getRawQuery()))
                .andReturn().getResponse().getStatus();
    }

    private User user(String name, boolean admin) {
        User u = User.of(name + "@example.test", encoder.encode(PASSWORD));
        u.setEnabled(true);
        if (admin) {
            u.getRoles().add(Role.ADMIN);
        }
        return users.save(u);
    }

    private String login(User user) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }
}

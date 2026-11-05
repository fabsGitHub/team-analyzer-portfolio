package com.teamanalyzer.teamanalyzer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.teamanalyzer.teamanalyzer.domain.Survey;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import com.teamanalyzer.teamanalyzer.service.SurveyService;
import com.teamanalyzer.teamanalyzer.service.TeamService;
import com.teamanalyzer.teamanalyzer.service.TokenService;

/**
 * Submitted answers must not be linkable to the member who received the participation token:
 * no token reference on the response, and only the submission date on response and answers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SurveyAnonymityIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UserRepository users;
    @Autowired
    TeamService teams;
    @Autowired
    SurveyService surveys;
    @Autowired
    TokenService tokens;

    @Test
    void response_cannot_be_joined_back_to_the_token_or_member() throws Exception {
        String run = Long.toString(System.nanoTime());
        User leader = users.save(User.of("lead-" + run + "@example.test", "x".repeat(60)));
        User member = users.save(User.of("member-" + run + "@example.test", "x".repeat(60)));

        var team = teams.createTeam("Team " + run, leader.getId());
        teams.addMember(team.getId(), member.getId(), false);
        Survey survey = surveys.createSurvey(leader.getId(), team.getId(), "Pulse " + run,
                List.of("Q1", "Q2", "Q3", "Q4", "Q5"));
        String plainToken = tokens.ensurePersonalToken(survey, member.getId(), member.getEmail());

        String body = """
                {"token":"%s","q1":5,"q2":4,"q3":3,"q4":2,"q5":1}""".formatted(plainToken);
        mvc.perform(post("/api/surveys/{id}/responses", survey.getId())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());

        // The token is single-use ...
        mvc.perform(post("/api/surveys/{id}/responses", survey.getId())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isGone());
        assertThat(jdbc.queryForObject(
                "select redeemed from survey_tokens where survey_id = uuid_to_bin(?)", Boolean.class,
                survey.getId().toString())).isTrue();

        // ... but the response keeps no reference to it.
        Integer tokenColumns = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                 where table_schema = database() and table_name = 'survey_responses'
                   and column_name like '%token%'""", Integer.class);
        assertThat(tokenColumns).isZero();

        // Only the submission date is stored, so times cannot be matched with the redemption.
        UUID surveyId = survey.getId();
        List<Long> responseSeconds = jdbc.queryForList("""
                select unix_timestamp(created_at) % 86400 from survey_responses
                 where survey_id = uuid_to_bin(?)
                union all
                select unix_timestamp(updated_at) % 86400 from survey_responses
                 where survey_id = uuid_to_bin(?)""", Long.class, surveyId.toString(), surveyId.toString());
        List<Long> answerSeconds = jdbc.queryForList("""
                select unix_timestamp(a.created_at) % 86400 from survey_answers a
                  join survey_responses r on r.id = a.response_id
                 where r.survey_id = uuid_to_bin(?)
                union all
                select unix_timestamp(a.updated_at) % 86400 from survey_answers a
                  join survey_responses r on r.id = a.response_id
                 where r.survey_id = uuid_to_bin(?)""", Long.class, surveyId.toString(), surveyId.toString());

        assertThat(responseSeconds).hasSize(2).containsOnly(0L);

        assertThat(answerSeconds).hasSize(10).containsOnly(0L);

        // Results are still complete.
        var results = surveys.getResults(surveyId);
        assertThat(results.n()).isEqualTo(1);
        assertThat(results.a1()).isEqualTo(5.0);
        assertThat(results.a5()).isEqualTo(1.0);
    }
}

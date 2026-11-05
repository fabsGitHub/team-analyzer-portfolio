package com.teamanalyzer.teamanalyzer.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.teamanalyzer.teamanalyzer.domain.Role;
import com.teamanalyzer.teamanalyzer.domain.Survey;
import com.teamanalyzer.teamanalyzer.domain.SurveyAnswer;
import com.teamanalyzer.teamanalyzer.domain.SurveyQuestion;
import com.teamanalyzer.teamanalyzer.domain.SurveyResponse;
import com.teamanalyzer.teamanalyzer.domain.Team;
import com.teamanalyzer.teamanalyzer.domain.TeamMember;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.repo.SurveyQuestionRepository;
import com.teamanalyzer.teamanalyzer.repo.SurveyRepository;
import com.teamanalyzer.teamanalyzer.repo.SurveyResponseRepository;
import com.teamanalyzer.teamanalyzer.repo.TeamMemberRepository;
import com.teamanalyzer.teamanalyzer.repo.TeamRepository;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoWorkspaceService {
  private static final Duration WORKSPACE_TTL = Duration.ofHours(24);
  private static final String DEMO_EMAIL_PATTERN = "demo+%@example.invalid";
  private static final List<String> QUESTIONS = List.of(
      "Our team is clear about its most important priorities.",
      "I can ask for help before a blocker becomes a problem.",
      "I have enough focus time to do my best work.",
      "Different perspectives are welcomed in our discussions.",
      "I understand how my work contributes to our goals.");

  private static final short[][] SAMPLE_RATINGS = {
      { 5, 4, 4, 4, 5 }, { 4, 5, 4, 4, 4 }, { 4, 4, 3, 4, 4 },
      { 3, 4, 3, 5, 4 }, { 5, 4, 5, 4, 5 }, { 4, 3, 3, 4, 4 },
      { 3, 4, 2, 4, 3 }, { 4, 5, 4, 5, 4 }, { 5, 4, 4, 3, 4 },
      { 4, 3, 4, 4, 4 }, { 3, 4, 3, 3, 4 }, { 5, 5, 4, 5, 5 },
      { 4, 4, 3, 4, 4 }, { 3, 3, 2, 4, 3 }, { 4, 4, 4, 5, 4 },
      { 5, 4, 5, 4, 5 }, { 4, 3, 4, 4, 3 }, { 3, 4, 3, 5, 4 }
  };

  private final PasswordEncoder passwordEncoder;
  private final UserRepository users;
  private final TeamRepository teams;
  private final TeamMemberRepository memberships;
  private final SurveyRepository surveys;
  private final SurveyQuestionRepository questions;
  private final SurveyResponseRepository responses;
  private final JdbcTemplate jdbc;

  @Transactional
  public DemoWorkspace createWorkspace() {
    UUID workspaceKey = UUID.randomUUID();
    String email = "demo+" + workspaceKey + "@example.invalid";
    User user = User.of(email, passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()));
    user.verifyEmailNow(Instant.now());
    user.getRoles().add(Role.LEADER);
    users.save(user);

    String suffix = workspaceKey.toString().substring(0, 8);
    Team team = teams.save(new Team("Demo team " + suffix));
    memberships.save(TeamMember.of(team, user, true));

    Survey survey = surveys.save(Survey.create(team, user.getId(), "Team pulse — " + suffix));
    List<SurveyQuestion> surveyQuestions = new ArrayList<>(QUESTIONS.size());
    for (int i = 0; i < QUESTIONS.size(); i++) {
      SurveyQuestion question = new SurveyQuestion();
      question.setSurvey(survey);
      question.setIdx((short) (i + 1));
      question.setText(QUESTIONS.get(i));
      surveyQuestions.add(question);
    }
    questions.saveAll(surveyQuestions);

    // Like real submissions, the sample responses carry only their submission date.
    Instant submittedAt = Instant.now();
    List<SurveyResponse> seededResponses = new ArrayList<>(SAMPLE_RATINGS.length);
    for (short[] ratings : SAMPLE_RATINGS) {
      SurveyResponse response = SurveyResponse.create(survey, submittedAt);
      for (int i = 0; i < surveyQuestions.size(); i++) {
        SurveyAnswer answer = new SurveyAnswer();
        answer.setQuestion(surveyQuestions.get(i));
        answer.setValue(ratings[i]);
        answer.setAnswerOrder(i);
        response.addAnswer(answer);
      }
      seededResponses.add(response);
    }
    responses.saveAll(seededResponses);

    return new DemoWorkspace(user, survey.getId());
  }

  /**
   * Deletes demo accounts older than 24 hours together with their teams (surveys, tokens,
   * responses and answers cascade). Only teams without any non-demo member are deleted, so a
   * demo account that was added to a real team never takes that team with it.
   */
  @Scheduled(fixedDelayString = "${app.demo.cleanup-interval-ms:3600000}")
  @Transactional
  public void cleanupExpiredWorkspaces() {
    Timestamp cutoff = Timestamp.from(Instant.now().minus(WORKSPACE_TTL));
    jdbc.update("""
        DELETE FROM teams
         WHERE id IN (
           SELECT team_id FROM (
             SELECT tm.team_id
               FROM team_members tm
               JOIN users u ON u.id = tm.user_id
              WHERE u.email LIKE ?
                AND u.created_at < ?
                AND NOT EXISTS (
                  SELECT 1
                    FROM team_members other
                    JOIN users ou ON ou.id = other.user_id
                   WHERE other.team_id = tm.team_id
                     AND ou.email NOT LIKE ?)
           ) expired_demo_teams
         )
        """, DEMO_EMAIL_PATTERN, cutoff, DEMO_EMAIL_PATTERN);
    // A demo account that still owns a survey in a kept team stays until that survey is gone.
    jdbc.update("""
        DELETE FROM users
         WHERE email LIKE ?
           AND created_at < ?
           AND NOT EXISTS (SELECT 1 FROM surveys s WHERE s.created_by = users.id)
        """, DEMO_EMAIL_PATTERN, cutoff);
  }

  public record DemoWorkspace(User user, UUID surveyId) {
  }
}

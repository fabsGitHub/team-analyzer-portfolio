package com.teamanalyzer.teamanalyzer.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.teamanalyzer.teamanalyzer.config.DemoProperties;
import com.teamanalyzer.teamanalyzer.domain.DemoAccounts;
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
import com.teamanalyzer.teamanalyzer.service.DemoLimitExceededException.Limit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates the seeded workspaces behind "Try the live demo". Expired ones: {@link DemoWorkspaceCleanup}. */
@Service
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoWorkspaceService {
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
  private final DemoProperties demo;

  /**
   * Creates a seeded workspace, unless the global limits are reached: at most
   * {@code maxLiveWorkspaces} demo accounts at a time and {@code maxSessionsPerHour} new ones per
   * hour. Both are counted in the database, so they hold across API instances. Concurrent starts
   * are not serialised, so a burst can pass a limit by the number of requests in flight.
   */
  @Transactional
  public DemoWorkspace createWorkspace() {
    ensureCapacity();
    UUID workspaceKey = UUID.randomUUID();
    String email = DemoAccounts.emailFor(workspaceKey);
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

  private void ensureCapacity() {
    // Every demo account in the table counts, expired or not: if cleanup falls behind, new demos
    // stop instead of the table growing.
    int live = count("SELECT COUNT(*) FROM users WHERE email LIKE ?");
    if (live >= demo.maxLiveWorkspaces()) {
      throw new DemoLimitExceededException(Limit.CAPACITY, demo.maxLiveWorkspaces(), Duration.ofHours(1));
    }
    int lastHour = count("""
        SELECT COUNT(*) FROM users
         WHERE email LIKE ?
           AND created_at >= CURRENT_TIMESTAMP - INTERVAL 1 HOUR""");
    if (lastHour >= demo.maxSessionsPerHour()) {
      throw new DemoLimitExceededException(Limit.HOURLY, demo.maxSessionsPerHour(), Duration.ofHours(1));
    }
  }

  private int count(String sql) {
    Integer n = jdbc.queryForObject(sql, Integer.class, DemoAccounts.EMAIL_LIKE);
    return n == null ? 0 : n;
  }

  public record DemoWorkspace(User user, UUID surveyId) {
  }
}

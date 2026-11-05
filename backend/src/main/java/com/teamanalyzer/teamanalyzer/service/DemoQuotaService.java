package com.teamanalyzer.teamanalyzer.service;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.config.DemoProperties;
import com.teamanalyzer.teamanalyzer.domain.DemoAccounts;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import com.teamanalyzer.teamanalyzer.service.DemoLimitExceededException.Limit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/**
 * Write quotas for demo accounts. A demo token can be used by anyone, so everything it can create
 * in its workspace is capped: surveys per team and participation tokens per survey (which also
 * caps the responses those tokens can submit).
 *
 * <p>The checks run inside the transaction that inserts the row. They lock the team row first,
 * so concurrent requests for one workspace are counted one after the other, and they count with
 * a locking read, which sees rows committed after the transaction's snapshot. Requests from other
 * accounts are not affected.
 */
@Service
@RequiredArgsConstructor
public class DemoQuotaService {

  private final DemoProperties demo;
  private final UserRepository users;
  private final JdbcTemplate jdbc;

  @PersistenceContext
  private EntityManager em;

  /** Before a survey is inserted: demo accounts keep at most {@code maxSurveysPerWorkspace}. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void claimSurvey(UUID creatorId, UUID teamId) {
    if (!users.existsByIdAndEmailLike(creatorId, DemoAccounts.EMAIL_LIKE)) {
      return;
    }
    lockTeam(bytes(teamId));
    int surveys = lockedRows("SELECT id FROM surveys WHERE team_id = ? FOR UPDATE", bytes(teamId));
    if (surveys >= demo.maxSurveysPerWorkspace()) {
      throw new DemoLimitExceededException(Limit.SURVEYS, demo.maxSurveysPerWorkspace());
    }
  }

  /**
   * Before tokens of a demo account are revoked for a renewal: takes the workspace lock first, so
   * every demo token write locks the team row before any token row.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void lockWorkspaceForTokens(UUID surveyId, String recipientEmail) {
    if (DemoAccounts.isDemoEmail(recipientEmail)) {
      lockTeamOfSurvey(surveyId);
    }
  }

  /** Before a token for a demo account is inserted: at most {@code maxTokensPerSurvey}. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void claimToken(UUID surveyId, String recipientEmail) {
    if (!DemoAccounts.isDemoEmail(recipientEmail)) {
      return;
    }
    lockTeamOfSurvey(surveyId);
    int tokens = lockedRows("SELECT id FROM survey_tokens WHERE survey_id = ? FOR UPDATE", bytes(surveyId));
    if (tokens >= demo.maxTokensPerSurvey()) {
      throw new DemoLimitExceededException(Limit.TOKENS, demo.maxTokensPerSurvey());
    }
  }

  private void lockTeamOfSurvey(UUID surveyId) {
    // Not one joined FOR UPDATE: locking the survey row too could deadlock with a submission,
    // which holds its token row and then needs a shared lock on the survey row for its insert.
    List<byte[]> team = jdbc.queryForList("SELECT team_id FROM surveys WHERE id = ?", byte[].class, bytes(surveyId));
    if (!team.isEmpty()) {
      lockTeam(team.get(0));
    }
  }

  private void lockTeam(byte[] teamId) {
    jdbc.queryForList("SELECT id FROM teams WHERE id = ? FOR UPDATE", byte[].class, (Object) teamId);
  }

  private int lockedRows(String sql, byte[] id) {
    // Rows this transaction persisted through JPA but has not written yet must be counted too.
    em.flush();
    return jdbc.queryForList(sql, byte[].class, (Object) id).size();
  }

  /** The BINARY(16) form Hibernate stores UUIDs in (most significant bits first). */
  private static byte[] bytes(UUID id) {
    return ByteBuffer.allocate(16)
        .putLong(id.getMostSignificantBits())
        .putLong(id.getLeastSignificantBits())
        .array();
  }
}

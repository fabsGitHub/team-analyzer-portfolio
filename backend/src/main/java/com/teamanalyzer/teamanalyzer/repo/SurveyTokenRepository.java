// backend/src/main/java/com/teamanalyzer/teamanalyzer/repo/SurveyTokenRepository.java
package com.teamanalyzer.teamanalyzer.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.SurveyToken;

import jakarta.persistence.LockModeType;

@Repository
@Transactional(readOnly = true)
public interface SurveyTokenRepository extends JpaRepository<SurveyToken, UUID> {

  long countBySurveyIdAndRedeemedAtIsNotNull(UUID surveyId);

  /**
   * Aktives Token (nicht eingelöst, nicht revoked) eines Users für ein Survey.
   * „First“ weil mehrere ausgestellt sein könnten — in der Praxis nach IssuedAt
   * indizieren.
   */
  Optional<SurveyToken> findFirstBySurvey_IdAndIssuedToUser_IdAndRedeemedFalseAndRevokedFalse(
      UUID surveyId, UUID userId);

  /** (Optional) Alle Tokens eines Users für Anzeigezwecke. */
  List<SurveyToken> findByIssuedToUser_IdAndSurvey_Id(UUID userId, UUID surveyId);

  /**
   * Offene Tokens eines Users samt Survey für Dashboards. JPQL für Sortierung +
   * Fetch Survey.
   */
  @Query("""
      select t
        from SurveyToken t
        join fetch t.survey s
       where t.issuedToUser.id = :userId
         and t.revoked = false
         and t.redeemed = false
       order by t.issuedAt desc
      """)
  List<SurveyToken> findOpenByUser(UUID userId);

  /**
   * Widerruft alle noch aktiven Tokens eines Users für ein Survey (z. B. bei
   * Neuausstellung).
   */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update SurveyToken t
         set t.revoked = true,
             t.revokedAt = CURRENT_TIMESTAMP
       where t.survey.id = :surveyId
         and t.issuedToUser.id = :userId
         and t.redeemed = false
         and t.revoked = false
      """)
  int revokeAllActiveForUser(UUID surveyId, UUID userId);

  /** Lookup beim Redeem (Hash + Survey). */
  Optional<SurveyToken> findByTokenHashAndSurvey_Id(byte[] tokenHash, UUID surveyId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<SurveyToken> findWithLockByTokenHashAndSurvey_Id(byte[] tokenHash, UUID surveyId);

}

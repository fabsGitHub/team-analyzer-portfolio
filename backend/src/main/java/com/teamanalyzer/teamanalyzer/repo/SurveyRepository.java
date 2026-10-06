// backend/src/main/java/com/teamanalyzer/teamanalyzer/repo/SurveyRepository.java
package com.teamanalyzer.teamanalyzer.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.Survey;

@Repository
@Transactional(readOnly = true)
public interface SurveyRepository extends JpaRepository<Survey, UUID> {

  /**
   * Scoping nach Team: schützt gegen versehentliche Querzugriffe (Bounded
   * Context).
   */
  Optional<Survey> findByIdAndTeamId(UUID surveyId, UUID teamId);

  /**
   * Existenzprüfungen für Zugriffsentscheidungen in der Service-Schicht.
   */
  boolean existsByIdAndTeam_Members_User_Id(UUID surveyId, UUID userId);

  boolean existsByIdAndTeam_Members_User_IdAndTeam_Members_LeaderTrue(UUID surveyId, UUID userId);

  /**
   * Surveys eines Users inkl. Fragen (verhindert N+1 in Listenern/Mappern).
   */
  @EntityGraph(attributePaths = { "questions" })
  List<Survey> findByCreatedBy(UUID createdBy);

  /**
   * Bevorzugt EntityGraph statt „join fetch“ + JPQL für Lesbarkeit & Wartbarkeit.
   * Sortierung über Methodennamen — offen für Erweiterung, geschlossen für
   * Modifikation (O aus SOLID).
   */
  @EntityGraph(attributePaths = { "team", "questions" })
  List<Survey> findByCreatedByOrderByIdDesc(UUID createdBy);

  /**
   * Leichte, performante Projektion: nur Team-ID eines Surveys.
   */
  @Query("select s.team.id from Survey s where s.id = :surveyId")
  Optional<UUID> findTeamIdById(UUID surveyId);

  @Query("""
        select distinct s
          from Survey s
          join fetch s.team t
          left join fetch s.questions q
         where s.createdBy = :createdBy
         order by s.id desc
      """)
  List<Survey> findByCreatedByWithTeamAndQuestions(UUID createdBy);
}

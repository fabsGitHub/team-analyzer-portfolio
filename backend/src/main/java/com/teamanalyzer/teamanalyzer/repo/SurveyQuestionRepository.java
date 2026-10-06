// backend/src/main/java/com/teamanalyzer/teamanalyzer/repo/SurveyQuestionRepository.java
package com.teamanalyzer.teamanalyzer.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.SurveyQuestion;

@Repository
@Transactional(readOnly = true)
public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, UUID> {

    /**
     * Fragen eines Surveys in der definierten Reihenfolge.
     * Bewusst einfache Methodensignatur für KISS.
     */
    List<SurveyQuestion> findBySurveyIdOrderByIdx(UUID surveyId);
}

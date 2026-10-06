// backend/src/main/java/com/teamanalyzer/teamanalyzer/repo/SurveyResponseRepository.java
package com.teamanalyzer.teamanalyzer.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.SurveyResponse;

@Repository
@Transactional(readOnly = true)
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, UUID> {

    @Query("""
            select distinct r
            from SurveyResponse r
            left join fetch r.answers a
            left join fetch a.question q
            where r.survey.id = :surveyId
            """)
    List<SurveyResponse> findBySurveyId(UUID surveyId);
}

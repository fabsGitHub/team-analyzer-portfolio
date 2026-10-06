package com.teamanalyzer.teamanalyzer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "survey_answers", uniqueConstraints = @UniqueConstraint(name = "uq_response_question", columnNames = {
        "response_id", "question_id" }))
public class SurveyAnswer extends UuidEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false)
    private SurveyResponse response;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private SurveyQuestion question;

    // Likert 1..5 (oder erweitern)
    @Column(name = "value", nullable = false)
    private short value;

    // NEW: make sure this exists and is insertable
    @Column(name = "answer_order", nullable = false)
    private Integer answerOrder;
}

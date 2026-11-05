package com.teamanalyzer.teamanalyzer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** A single Likert answer. Timestamps are the response's coarse submission date (see {@link SurveyResponse}). */
@Entity
@Getter
@Setter
@Table(name = "survey_answers", uniqueConstraints = @UniqueConstraint(name = "uq_response_question", columnNames = {
        "response_id", "question_id" }))
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false, updatable = false))
})
public class SurveyAnswer extends UuidEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false)
    private SurveyResponse response;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private SurveyQuestion question;

    // Likert value 1..5
    @Column(name = "value", nullable = false)
    private short value;

    @Column(name = "answer_order", nullable = false)
    private Integer answerOrder;
}

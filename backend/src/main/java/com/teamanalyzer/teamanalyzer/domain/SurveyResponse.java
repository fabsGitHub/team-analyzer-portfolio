package com.teamanalyzer.teamanalyzer.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * One anonymous set of answers to a survey.
 *
 * <p>A response deliberately has no reference to the participation token or the member who
 * answered, and stores only the submission <em>date</em> (UTC midnight) on itself and its
 * answers, so the application tables cannot match it with the precise redemption time on the
 * token. Database transaction logs (e.g. the MySQL binary log) are outside the application's
 * control; see the README's security notes.
 */
@Entity
@Getter
@Table(name = "survey_responses")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false, updatable = false))
})
public class SurveyResponse extends UuidEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Survey survey;

    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("answerOrder ASC")
    private final List<SurveyAnswer> answers = new ArrayList<>();

    protected SurveyResponse() {
    }

    /**
     * @param submittedAt the submission time; only its UTC date is stored
     */
    public static SurveyResponse create(Survey survey, Instant submittedAt) {
        SurveyResponse sr = new SurveyResponse();
        sr.survey = survey;
        sr.useFixedTimestamps(submittedAt.truncatedTo(ChronoUnit.DAYS));
        return sr;
    }

    /** Links an answer to this response; it inherits the response's coarse timestamp. */
    public void addAnswer(SurveyAnswer answer) {
        answers.add(answer);
        answer.setResponse(this);
        answer.useFixedTimestamps(getCreatedAt());
    }

    /** Value for a question id, if answered. */
    public Optional<Short> findValue(UUID questionId) {
        return answers.stream()
                .filter(a -> a.getQuestion() != null && questionId.equals(a.getQuestion().getId()))
                .map(SurveyAnswer::getValue)
                .findFirst();
    }

    /** Value by question index (1-based), useful when the UI expects a fixed order. */
    public Optional<Short> findValueByIdx(int idx) {
        return answers.stream()
                .filter(a -> a.getQuestion() != null && a.getQuestion().getIdx() == idx)
                .map(SurveyAnswer::getValue)
                .findFirst();
    }

    /** Answers sorted by question index. */
    public List<SurveyAnswer> getAnswersSortedByQuestionIdx() {
        return answers.stream()
                .sorted(Comparator.comparing(
                        a -> a.getQuestion() == null ? Integer.MAX_VALUE : a.getQuestion().getIdx()))
                .toList();
    }
}

package com.teamanalyzer.teamanalyzer.domain;

import java.util.*;
import java.util.function.Function;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "survey_responses")
public class SurveyResponse extends UuidEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_id")
    private SurveyToken token;

    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("answerOrder ASC")
    private final List<SurveyAnswer> answers = new ArrayList<>();

    protected SurveyResponse() {
    }

    private SurveyResponse(Survey survey, SurveyToken token) {
        this.survey = survey;
        this.token = token;
    }

    public static SurveyResponse create(Survey survey, SurveyToken token) {
        SurveyResponse sr = new SurveyResponse(survey, token);
        return sr;
    }

    /** Bidirektional verknüpfen */
    public void addAnswer(SurveyAnswer answer) {
        answers.add(answer); // fügt am Ende ein; Reihenfolge bleibt stabil
        answer.setResponse(this);
    }

    /** Bequemer Setter: Antwort für eine bestimmte Frage setzen/überschreiben */
    public void putAnswer(SurveyQuestion question, short value) {
        for (SurveyAnswer a : answers) {
            if (a.getQuestion().equals(question)) {
                a.setValue(value);
                return;
            }
        }
        SurveyAnswer a = new SurveyAnswer();
        a.setQuestion(question);
        a.setValue(value);
        addAnswer(a);
    }

    /** Optionalen Wert zu Frage-ID holen */
    public Optional<Short> findValue(UUID questionId) {
        return answers.stream()
                .filter(a -> a.getQuestion() != null && questionId.equals(a.getQuestion().getId()))
                .map(SurveyAnswer::getValue)
                .findFirst();
    }

    /**
     * Wert nach Frage-Index (idx) – nützlich, wenn UI feste Reihenfolge erwartet
     */
    public Optional<Short> findValueByIdx(int idx) {
        return answers.stream()
                .filter(a -> a.getQuestion() != null && a.getQuestion().getIdx() == idx)
                .map(SurveyAnswer::getValue)
                .findFirst();
    }

    /** Optional: sortierte Sicht nach question.idx (falls benötigt) */
    public List<SurveyAnswer> getAnswersSortedByQuestionIdx() {
        return answers.stream()
                .sorted(Comparator.comparing(
                        a -> a.getQuestion() == null ? Integer.MAX_VALUE : a.getQuestion().getIdx()))
                .toList();
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }

    public void setToken(SurveyToken token) {
        this.token = token;
    }

    public void clearToken() {
        this.token = null;
    }

    /** Factory: aus Map<Question,Value> */
    public static SurveyResponse of(Survey survey, SurveyToken tok, Map<SurveyQuestion, Short> values) {
        SurveyResponse r = new SurveyResponse(survey, tok);
        values.forEach(r::putAnswer);
        return r;
    }

    /** Factory: aus Map<QuestionId,Value> */
    public static SurveyResponse ofIds(
            Survey survey,
            SurveyToken tok,
            Map<UUID, Short> values,
            Function<UUID, SurveyQuestion> refLoader) {
        SurveyResponse r = new SurveyResponse(survey, tok);
        values.forEach((qid, val) -> r.putAnswer(refLoader.apply(qid), val));
        return r;
    }
}

package com.teamanalyzer.teamanalyzer.web.dto;

import java.util.List;
import java.util.UUID;

import com.teamanalyzer.teamanalyzer.domain.Survey;
import com.teamanalyzer.teamanalyzer.domain.SurveyQuestion;

public record SurveyDto(
        UUID id,
        String title,
        UUID createdBy,
        String teamName,
        List<QuestionDto> questions) {
    public static record QuestionDto(UUID id, short idx, String text) {
    }

    public static SurveyDto from(Survey s, List<SurveyQuestion> qs) {
        var questions = qs.stream()
                .map(q -> new QuestionDto(q.getId(), q.getIdx(), q.getText()))
                .toList();

        return new SurveyDto(
                s.getId(),
                s.getTitle(),
                s.getCreatedBy(),
                s.getTeam().getName(),
                questions);
    }
}

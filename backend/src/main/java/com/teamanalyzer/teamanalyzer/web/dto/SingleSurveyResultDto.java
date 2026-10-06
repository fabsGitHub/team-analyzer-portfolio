package com.teamanalyzer.teamanalyzer.web.dto;

import java.util.Map;
import java.util.UUID;

import com.teamanalyzer.teamanalyzer.domain.SurveyAnswer;
import com.teamanalyzer.teamanalyzer.domain.SurveyResponse;

public record SingleSurveyResultDto(UUID responseId, Map<UUID, Short> answers) {
    public static SingleSurveyResultDto from(SurveyResponse r) {
        Map<UUID, Short> map = r.getAnswers().stream()
                .filter(a -> a.getQuestion() != null)
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getQuestion().getId(),
                        SurveyAnswer::getValue));
        return new SingleSurveyResultDto(r.getId(), map);
    }
}

package com.teamanalyzer.teamanalyzer.web.dto;

import java.util.List;

public record SurveyResultsDto(
        double a1, double a2, double a3, double a4, double a5,
        int n,
        List<SingleSurveyResultDto> items) {
    public static SurveyResultsDto of(double a1, double a2, double a3, double a4, double a5, int n,
            List<SingleSurveyResultDto> items) {
        return new SurveyResultsDto(a1, a2, a3, a4, a5, n, items);
    }
}
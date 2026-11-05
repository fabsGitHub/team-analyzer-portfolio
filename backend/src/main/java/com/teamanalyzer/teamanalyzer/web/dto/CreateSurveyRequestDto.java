// CreateSurveyRequest.java
package com.teamanalyzer.teamanalyzer.web.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

/** Lengths match the database columns ({@code surveys.title}, {@code survey_questions.text}). */
public record CreateSurveyRequestDto(
        @NotNull UUID teamId,
        @NotBlank @Size(max = CreateSurveyRequestDto.MAX_TITLE_LENGTH) String title,
        @Size(min = 5, max = 5) List<@NotBlank @Size(max = CreateSurveyRequestDto.MAX_QUESTION_LENGTH) String> questions) {

    public static final int MAX_TITLE_LENGTH = 300;
    public static final int MAX_QUESTION_LENGTH = 1000;
}

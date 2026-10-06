// CreateSurveyRequest.java
package com.teamanalyzer.teamanalyzer.web.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record CreateSurveyRequestDto(
        @NotNull UUID teamId,
        @NotBlank String title,
        @Size(min = 5, max = 5) List<@NotBlank String> questions) {
}

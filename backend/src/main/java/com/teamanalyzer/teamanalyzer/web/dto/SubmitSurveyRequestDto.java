// SubmitSurveyRequest.java
package com.teamanalyzer.teamanalyzer.web.dto;

import jakarta.validation.constraints.*;

public record SubmitSurveyRequestDto(
        @NotBlank String token,
        @Min(1) @Max(5) short q1,
        @Min(1) @Max(5) short q2,
        @Min(1) @Max(5) short q3,
        @Min(1) @Max(5) short q4,
        @Min(1) @Max(5) short q5) {
}

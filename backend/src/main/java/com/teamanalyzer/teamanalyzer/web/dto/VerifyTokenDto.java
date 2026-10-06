package com.teamanalyzer.teamanalyzer.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyTokenDto(@NotBlank String token) {
}

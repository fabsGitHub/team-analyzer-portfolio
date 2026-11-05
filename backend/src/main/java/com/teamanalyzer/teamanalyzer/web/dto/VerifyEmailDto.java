package com.teamanalyzer.teamanalyzer.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirmation link token plus the password chosen at registration. */
public record VerifyEmailDto(@NotBlank String token, @NotBlank String password) {
}

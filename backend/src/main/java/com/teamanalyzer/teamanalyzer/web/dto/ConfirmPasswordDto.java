package com.teamanalyzer.teamanalyzer.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmPasswordDto(@NotBlank String token, @NotBlank @Size(min = 10) String newPassword) {
}

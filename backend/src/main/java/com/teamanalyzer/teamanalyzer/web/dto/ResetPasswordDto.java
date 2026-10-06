package com.teamanalyzer.teamanalyzer.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordDto(
    @NotBlank @Email String email
) {}

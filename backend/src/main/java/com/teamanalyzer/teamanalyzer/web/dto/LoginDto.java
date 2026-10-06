package com.teamanalyzer.teamanalyzer.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginDto(@NotBlank @Email String email, String password) {
}

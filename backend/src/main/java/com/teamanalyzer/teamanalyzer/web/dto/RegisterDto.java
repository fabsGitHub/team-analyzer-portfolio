package com.teamanalyzer.teamanalyzer.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDto(@NotBlank @Email String email, @Size(min = 10) String password) {
}

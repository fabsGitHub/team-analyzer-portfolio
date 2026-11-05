package com.teamanalyzer.teamanalyzer.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * BCrypt only uses the first 72 bytes of a password, so longer input is rejected. Addresses with
 * invisible, control or separator characters are rejected so look-alike accounts cannot be made.
 */
public record RegisterDto(
        @NotBlank @Email @Size(max = 254)
        @Pattern(regexp = "[^\\p{Cc}\\p{Cf}\\p{Z}]+", message = "must not contain invisible or space characters")
        String email,
        @NotBlank @Size(min = 10, max = 72) String password) {
}

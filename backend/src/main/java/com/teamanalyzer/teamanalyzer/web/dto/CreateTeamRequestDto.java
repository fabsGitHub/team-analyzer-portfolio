package com.teamanalyzer.teamanalyzer.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTeamRequestDto(@NotBlank @Size(max = 200) String name, @NotNull UUID leaderUserId) {
}

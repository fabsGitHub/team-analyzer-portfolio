package com.teamanalyzer.teamanalyzer.web.dto;

import java.util.UUID;

public record CreateTeamRequestDto(String name, UUID leaderUserId) {
}
package com.teamanalyzer.teamanalyzer.web.dto;

import java.util.List;
import java.util.UUID;

import com.teamanalyzer.teamanalyzer.domain.Team;

public record TeamAdminDto(UUID id, String name, List<Member> members) {
    record Member(UUID userId, boolean leader) {
    }

    public static TeamAdminDto fromEntity(Team t) {
        List<Member> members = t.getMembers().stream()
                .map(m -> new Member(m.getUser().getId(), m.isLeader()))
                .toList();
        return new TeamAdminDto(t.getId(), t.getName(), members);
    }
}

// MeController.java
package com.teamanalyzer.teamanalyzer.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.teamanalyzer.teamanalyzer.security.AuthUser;
import com.teamanalyzer.teamanalyzer.web.dto.SurveyDto;
import com.teamanalyzer.teamanalyzer.repo.SurveyRepository;
import com.teamanalyzer.teamanalyzer.repo.TeamLiteView;
import com.teamanalyzer.teamanalyzer.repo.TeamMemberRepository;
import com.teamanalyzer.teamanalyzer.repo.TeamRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final SurveyRepository surveyRepository;

    @GetMapping()
    public Map<String, Object> me(@AuthenticationPrincipal AuthUser me) {
        if (me == null || me.userId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        boolean isLeader = teamMemberRepository.existsByUser_IdAndLeaderTrue(me.userId());

        return Map.of(
                "id", me.userId().toString(),
                "email", me.email(),
                "roles", me.roles(), // kommt direkt aus deinem AuthUser
                "isLeader", isLeader);
    }

    @GetMapping("/teams")
    public List<TeamLiteView> myTeams(@AuthenticationPrincipal AuthUser me,
            @RequestParam(defaultValue = "false") boolean leaderOnly) {
        if (me == null || me.userId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return leaderOnly
                ? teamRepository.findDistinctByMembers_User_IdAndMembers_LeaderTrue(me.userId())
                : teamRepository.findDistinctByMembers_User_Id(me.userId());
    }

    @GetMapping("/surveys")
    @Transactional(readOnly = true)
    public List<SurveyDto> getMySurveys(@AuthenticationPrincipal AuthUser me) {
        if (me == null || me.userId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        UUID userId = me.userId();
        return surveyRepository.findByCreatedByWithTeamAndQuestions(userId).stream()
                .map(s -> SurveyDto.from(s, s.getQuestions()))
                .toList();
    }
}

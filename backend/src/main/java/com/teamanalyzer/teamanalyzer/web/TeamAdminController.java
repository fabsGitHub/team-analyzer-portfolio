// src/main/java/com/teamanalyzer/teamanalyzer/web/TeamAdminController.java
package com.teamanalyzer.teamanalyzer.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.teamanalyzer.teamanalyzer.domain.Role;
import com.teamanalyzer.teamanalyzer.domain.Team;
import com.teamanalyzer.teamanalyzer.domain.TeamMember;
import com.teamanalyzer.teamanalyzer.domain.TeamMemberKey;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.repo.TeamMemberRepository;
import com.teamanalyzer.teamanalyzer.repo.TeamRepository;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import com.teamanalyzer.teamanalyzer.service.TeamService;
import com.teamanalyzer.teamanalyzer.web.dto.CreateTeamRequestDto;
import com.teamanalyzer.teamanalyzer.web.dto.TeamAdminDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/teams")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TeamAdminController {

    // Request-Body für Member-Update
    public record UpdateMemberRequest(boolean leader) {
    }

    private final TeamService teamService;

    private final TeamRepository teamRepo;
    private final UserRepository userRepo;
    private final TeamMemberRepository teamMemberRepo;

    @PostMapping
    public ResponseEntity<Team> create(@RequestBody CreateTeamRequestDto body) {
        var team = teamService.createTeam(body.name(), body.leaderUserId());
        return ResponseEntity.created(URI.create("/api/admin/teams/" + team.getId()))
                .body(team);
    }

    @GetMapping
    public List<TeamAdminDto> list() {
        return teamRepo.findAll().stream()
                .map(TeamAdminDto::fromEntity)
                .toList();
    }

    @PutMapping("/{teamId}/members/{userId}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upsertMember(@PathVariable UUID teamId,
            @PathVariable UUID userId,
            @RequestBody UpdateMemberRequest body) {

        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var key = new TeamMemberKey(teamId, userId);
        var member = teamMemberRepo.findById(key).orElseGet(() -> {
            TeamMember m = TeamMember.of(team, user, false);
            return m;
        });

        boolean prevLeader = member.isLeader();
        member.setLeader(body.leader());
        teamMemberRepo.save(member);

        if (prevLeader != body.leader() || member.getCreatedAt() == null) {
            syncLeaderRole(user);
        }
    }

    @DeleteMapping("{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void removeMember(@PathVariable UUID teamId, @PathVariable UUID userId) {
        TeamMemberKey key = new TeamMemberKey(teamId, userId);
        TeamMember member = teamMemberRepo.findById(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member not in team"));

        User user = member.getUser();
        teamMemberRepo.delete(member);

        syncLeaderRole(user);
    }

    @PatchMapping("{teamId}/leader")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void setLeader(@PathVariable UUID teamId,
            @RequestParam UUID userId,
            @RequestParam boolean leader) {

        TeamMemberKey key = new TeamMemberKey(teamId, userId);
        TeamMember m = teamMemberRepo.findById(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member not in team"));

        m.setLeader(leader);
        teamMemberRepo.save(m);

        User user = m.getUser();
        syncLeaderRole(user);
    }

    @DeleteMapping("/{teamId}")
    @Transactional
    public ResponseEntity<Void> deleteTeam(@PathVariable UUID teamId) {
        // Betroffene User vorher sammeln
        List<User> members = teamMemberRepo.findByTeam_Id(teamId)
                .stream().map(TeamMember::getUser).distinct().toList();

        teamMemberRepo.deleteByTeam_Id(teamId);
        teamRepo.deleteById(teamId);

        // Für jeden betroffenen User Leader-Rolle neu bewerten
        for (User u : members) {
            syncLeaderRole(u);
        }
        return ResponseEntity.noContent().build();
    }

    private void syncLeaderRole(User user) {
        UUID uid = user.getId();
        boolean leaderSomewhere = teamMemberRepo.existsByUser_IdAndLeaderTrue(uid);

        // Regel: Sobald kein Leader mehr (oder gar kein Mitglied mehr), Rolle
        // entfernen.
        // Wenn irgendwo Leader, Rolle setzen.
        if (leaderSomewhere) {
            if (user.getRoles().add(Role.LEADER)) {
                userRepo.save(user);
            }
        } else {
            if (user.getRoles().remove(Role.LEADER)) {
                userRepo.save(user);
            }
        }

    }

}

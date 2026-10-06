// src/test/java/com/teamanalyzer/teamanalyzer/web/TeamAdminControllerWebMvcTest.java
package com.teamanalyzer.teamanalyzer.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;

import com.teamanalyzer.teamanalyzer.config.SecurityConfig;
import com.teamanalyzer.teamanalyzer.domain.Role;
import com.teamanalyzer.teamanalyzer.domain.Team;
import com.teamanalyzer.teamanalyzer.domain.TeamMember;
import com.teamanalyzer.teamanalyzer.domain.TeamMemberKey;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.filter.JwtAuthFilter;
import com.teamanalyzer.teamanalyzer.port.AppClock;
import com.teamanalyzer.teamanalyzer.repo.TeamMemberRepository;
import com.teamanalyzer.teamanalyzer.repo.TeamRepository;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import com.teamanalyzer.teamanalyzer.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
// Option A: neues Field-basiertes MockitoBean (empfohlen ab Boot 3.4+)
// Falls das Paket bei dir (noch) nicht vorhanden ist, nutze unten Option B (@MockBean)
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
// CSRF Helper
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = TeamAdminController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
    SecurityConfig.class, JwtAuthFilter.class }))
@AutoConfigureMockMvc(addFilters = false)
class TeamAdminControllerWebMvcTest {

  @Autowired
  MockMvc mvc;

  // Option A: @MockitoBean (neuer Mechanismus)
  @MockitoBean
  TeamService teamService;
  @MockitoBean
  TeamRepository teamRepo;
  @MockitoBean
  UserRepository userRepo;
  @MockitoBean
  TeamMemberRepository teamMemberRepo;

  @MockitoBean
  AppClock appClock;

  // Option B (falls A nicht verfügbar): ersetze die vier @MockitoBean durch:
  // @MockBean TeamService teamService;
  // @MockBean TeamRepository teamRepo;
  // @MockBean UserRepository userRepo;
  // @MockBean TeamMemberRepository teamMemberRepo;

  UUID teamId;
  UUID u1; // initialer Leader
  UUID u2; // weiteres Mitglied

  Team team;
  User user1;
  User user2;

  @BeforeEach
  void init() {
    teamId = UUID.randomUUID();
    u1 = UUID.randomUUID();
    u2 = UUID.randomUUID();

    team = new Team("Blue");
    ReflectionTestUtils.setField(team, "id", teamId);

    // User als Mocks mit realem Rollen-Set
    user1 = mock(User.class);
    when(user1.getId()).thenReturn(u1);
    when(user1.getRoles()).thenReturn(new HashSet<>());

    user2 = mock(User.class);
    when(user2.getId()).thenReturn(u2);
    when(user2.getRoles()).thenReturn(new HashSet<>());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void full_admin_flow_create_upsert_leader_patch_delete_member() throws Exception {
    // --- 1) POST /api/admin/teams ---
    when(teamService.createTeam(eq("Blue"), eq(u1))).thenReturn(team);

    mvc.perform(post("/api/admin/teams")
        .with(csrf()) // <— wichtig
        .contentType(MediaType.APPLICATION_JSON)
        // vermeidet Textblock-Probleme in IDEs: kein """ ... """.formatted(...)
        .content("{\"name\":\"Blue\",\"leaderUserId\":\"" + u1 + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/admin/teams/" + teamId))
        .andExpect(jsonPath("$.name").value("Blue"))
        .andExpect(jsonPath("$.id").value(teamId.toString()));

    // --- 2) PUT upsert member (u2, leader=true) -> syncLeaderRole setzt Rolle ---
    when(teamRepo.findById(teamId)).thenReturn(Optional.of(team));
    when(userRepo.findById(u2)).thenReturn(Optional.of(user2));
    TeamMemberKey k12 = new TeamMemberKey(teamId, u2);
    when(teamMemberRepo.findById(k12)).thenReturn(Optional.empty()); // neu
    when(teamMemberRepo.existsByUser_IdAndLeaderTrue(u2)).thenReturn(true);

    clearInvocations(userRepo); // optional, isoliert diese Phase
    mvc.perform(put("/api/admin/teams/{teamId}/members/{userId}", teamId, u2)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"leader\":true}"))
        .andExpect(status().isNoContent());

    verify(userRepo, atLeastOnce()).save(any(User.class));
    assertThat(user2.getRoles()).contains(Role.LEADER);

    // --- 3) PATCH demote u1 leader=false ---
    TeamMember m11 = mock(TeamMember.class);
    when(m11.getUser()).thenReturn(user1);
    when(teamMemberRepo.findById(new TeamMemberKey(teamId, u1)))
        .thenReturn(Optional.of(m11));
    // nach Demote: u1 nirgendwo mehr Leader
    when(teamMemberRepo.existsByUser_IdAndLeaderTrue(u1)).thenReturn(false);

    clearInvocations(userRepo);
    mvc.perform(patch("/api/admin/teams/{teamId}/leader", teamId)
        .with(csrf())
        .param("userId", u1.toString())
        .param("leader", "false"))
        .andExpect(status().isNoContent());

    verify(userRepo, atLeastOnce()).save(any(User.class));
    assertThat(user1.getRoles()).doesNotContain(Role.LEADER);

    // --- 4) DELETE member u2 -> Rolle entfernen ---
    TeamMember m12 = mock(TeamMember.class);
    when(m12.getUser()).thenReturn(user2);
    when(teamMemberRepo.findById(k12)).thenReturn(Optional.of(m12));
    when(teamMemberRepo.existsByUser_IdAndLeaderTrue(u2)).thenReturn(false);

    clearInvocations(userRepo);
    mvc.perform(delete("/api/admin/teams/{teamId}/members/{userId}", teamId, u2)
        .with(csrf()))
        .andExpect(status().isNoContent());

    verify(userRepo, atLeastOnce()).save(any(User.class));
    assertThat(user2.getRoles()).doesNotContain(Role.LEADER);
  }
}

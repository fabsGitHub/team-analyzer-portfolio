package com.teamanalyzer.teamanalyzer.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import com.teamanalyzer.teamanalyzer.domain.Team;
import com.teamanalyzer.teamanalyzer.domain.TeamMember;
import com.teamanalyzer.teamanalyzer.domain.TeamMemberKey;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.repo.TeamMemberRepository;
import com.teamanalyzer.teamanalyzer.repo.TeamRepository;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    TeamRepository teamRepo;
    @Mock
    TeamMemberRepository tmRepo;
    @Mock
    UserRepository userRepo;

    @InjectMocks
    TeamService service;

    UUID teamId;
    UUID leaderId;
    UUID userId;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        leaderId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void createTeam_createsTeam_andAddsLeader() {
        // arrange
        // Der Service erstellt selbst "new Team(name)" und ruft save(t) -> wir
        // injizieren die ID via Reflection
        when(teamRepo.save(any(Team.class))).thenAnswer(inv -> {
            Team t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", teamId); // private Feld setzen
            return t;
        });

        // addMember() verwendet im Anschluss getReferenceById(...)
        Team teamRef = mock(Team.class);
        when(teamRepo.getReferenceById(teamId)).thenReturn(teamRef);

        User leaderRef = mock(User.class);
        when(leaderRef.getId()).thenReturn(leaderId);
        when(userRepo.getReferenceById(leaderId)).thenReturn(leaderRef);

        // act
        Team result = service.createTeam("Blue", leaderId);

        // assert
        assertThat(result.getId()).isEqualTo(teamId);
        assertThat(result.getName()).isEqualTo("Blue");

        // Das vom Service erzeugte TeamMember capturen und prüfen
        ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
        verify(tmRepo).save(captor.capture());
        TeamMember tm = captor.getValue();
        assertThat(tm.getTeam()).isSameAs(teamRef);
        assertThat(tm.getUser()).isSameAs(leaderRef);
        assertThat(tm.isLeader()).isTrue();
    }

    @Test
    void addMember_savesMember_withLeaderFlag() {
        // arrange
        Team teamRef = mock(Team.class);
        User userRef = mock(User.class);
        when(teamRepo.getReferenceById(teamId)).thenReturn(teamRef);
        when(userRepo.getReferenceById(userId)).thenReturn(userRef);

        // act
        service.addMember(teamId, userId, true);

        // assert
        ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
        verify(tmRepo).save(captor.capture());
        TeamMember tm = captor.getValue();
        assertThat(tm.getTeam()).isSameAs(teamRef);
        assertThat(tm.getUser()).isSameAs(userRef);
        assertThat(tm.isLeader()).isTrue();
    }

    @Test
    void setLeader_demoteLastLeader_throwsConflict() {
        // arrange
        TeamMemberKey key = new TeamMemberKey(teamId, leaderId);
        TeamMember tm = mock(TeamMember.class);
        when(tmRepo.findById(key)).thenReturn(Optional.of(tm));
        when(tmRepo.countByTeam_IdAndLeaderTrue(teamId)).thenReturn(1L); // letzter Leader

        // act + assert
        assertThatThrownBy(() -> service.setLeader(teamId, leaderId, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at least one leader");

        verify(tmRepo, never()).save(any());
    }

    @Test
    void setLeader_promoteMember_updatesFlag() {
        // arrange
        TeamMemberKey key = new TeamMemberKey(teamId, userId);
        TeamMember tm = mock(TeamMember.class);
        when(tmRepo.findById(key)).thenReturn(Optional.of(tm));

        // act
        service.setLeader(teamId, userId, true);

        // assert
        verify(tm).setLeader(true);
        verify(tmRepo).save(tm);
    }

    @Test
    void removeMember_removesNonLeader() {
        // arrange
        TeamMemberKey key = new TeamMemberKey(teamId, userId);
        TeamMember tm = mock(TeamMember.class);
        when(tm.isLeader()).thenReturn(false);
        when(tmRepo.findById(key)).thenReturn(Optional.of(tm));

        // act
        service.removeMember(teamId, userId);

        // assert
        verify(tmRepo).deleteById(key);
    }

    @Test
    void removeMember_lastLeader_throwsConflict() {
        // arrange
        TeamMemberKey key = new TeamMemberKey(teamId, leaderId);
        TeamMember tm = mock(TeamMember.class);
        when(tm.isLeader()).thenReturn(true);
        when(tmRepo.findById(key)).thenReturn(Optional.of(tm));
        when(tmRepo.countByTeam_IdAndLeaderTrue(teamId)).thenReturn(1L);

        // act + assert
        assertThatThrownBy(() -> service.removeMember(teamId, leaderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at least one leader");

        verify(tmRepo, never()).deleteById(any());
    }

    @Test
    void removeMember_noop_whenNotFound() {
        // arrange
        TeamMemberKey key = new TeamMemberKey(teamId, userId);
        when(tmRepo.findById(key)).thenReturn(Optional.empty());

        // act
        service.removeMember(teamId, userId);

        // assert
        verify(tmRepo, never()).deleteById(any());
    }
}

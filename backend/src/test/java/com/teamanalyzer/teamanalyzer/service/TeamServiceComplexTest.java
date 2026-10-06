// src/test/java/com/teamanalyzer/teamanalyzer/service/TeamServiceComplexTest.java
package com.teamanalyzer.teamanalyzer.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
class TeamServiceComplexTest {

    @Mock
    TeamRepository teamRepo;
    @Mock
    TeamMemberRepository tmRepo;
    @Mock
    UserRepository userRepo;

    @InjectMocks
    TeamService service;

    UUID teamId;
    UUID uLeader; // initialer Leader
    UUID uAlice; // weiteres Mitglied
    UUID uBob; // weiteres Mitglied

    // simpler in-memory Zustand, um Repository-Seite zu simulieren
    Map<TeamMemberKey, TeamMember> db = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        uLeader = UUID.randomUUID();
        uAlice = UUID.randomUUID();
        uBob = UUID.randomUUID();

        // teamRepo.save(team) -> ID „persistieren“
        when(teamRepo.save(any(Team.class))).thenAnswer(inv -> {
            Team t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", teamId);
            return t;
        });

        // getReferenceById(...) erzeugt Referenzen mit ID
        Team teamRef = mock(Team.class);
        when(teamRef.getId()).thenReturn(teamId);
        when(teamRepo.getReferenceById(teamId)).thenReturn(teamRef);

        User leaderRef = mock(User.class);
        when(leaderRef.getId()).thenReturn(uLeader);
        when(userRepo.getReferenceById(uLeader)).thenReturn(leaderRef);

        User aliceRef = mock(User.class);
        when(aliceRef.getId()).thenReturn(uAlice);
        when(userRepo.getReferenceById(uAlice)).thenReturn(aliceRef);

        User bobRef = mock(User.class);
        when(bobRef.getId()).thenReturn(uBob);
        when(userRepo.getReferenceById(uBob)).thenReturn(bobRef);

        // tmRepo.save -> in-memory db aktualisieren
        when(tmRepo.save(any(TeamMember.class))).thenAnswer(inv -> {
            TeamMember tm = inv.getArgument(0);
            // ID muss gesetzt sein (TeamMember.of macht das)
            db.put(tm.getId(), tm);
            return tm;
        });

        // tmRepo.findById / deleteById / countByTeam_IdAndLeaderTrue
        when(tmRepo.findById(any(TeamMemberKey.class)))
                .thenAnswer(inv -> Optional.ofNullable(db.get(inv.getArgument(0))));
        doAnswer(inv -> {
            TeamMemberKey key = inv.getArgument(0);
            db.remove(key);
            return null;
        }).when(tmRepo).deleteById(any(TeamMemberKey.class));
        when(tmRepo.countByTeam_IdAndLeaderTrue(any(UUID.class)))
                .thenAnswer(inv -> db.values().stream()
                        .filter(tm -> tm.getId().getTeamId().equals(inv.getArgument(0)))
                        .filter(TeamMember::isLeader)
                        .count());
    }

    @Test
    void fullLifecycle_works_withLeaderConstraints() {
        // 1) Team erstellen -> initialen Leader anlegen
        Team created = service.createTeam("Blue", uLeader);
        assertThat(created.getId()).isEqualTo(teamId);
        assertThat(db.values().stream().filter(TeamMember::isLeader).count()).isEqualTo(1);

        // 2) Zwei Mitglieder hinzufügen (ohne Leader)
        service.addMember(teamId, uAlice, false);
        service.addMember(teamId, uBob, false);
        assertThat(db).hasSize(3);
        assertThat(countLeaders()).isOne();

        // 3) uAlice zum Leader machen -> jetzt 2 Leader
        service.setLeader(teamId, uAlice, true);
        assertThat(countLeaders()).isEqualTo(2);

        // 4) initialen Leader demoten -> weiterhin 1 Leader vorhanden (uAlice)
        service.setLeader(teamId, uLeader, false);
        assertThat(countLeaders()).isOne();

        // 5) Versuch: letzten Leader demoten -> 409
        assertThatThrownBy(() -> service.setLeader(teamId, uAlice, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at least one leader");
        assertThat(countLeaders()).isOne(); // unverändert

        // 6) Versuch: letzten Leader entfernen -> 409
        assertThatThrownBy(() -> service.removeMember(teamId, uAlice))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at least one leader");
        assertThat(db).hasSize(3);

        // 7) zusätzlichen Leader ernennen (uBob), dann uAlice entfernen
        service.setLeader(teamId, uBob, true);
        assertThat(countLeaders()).isEqualTo(2);

        service.removeMember(teamId, uAlice);
        assertThat(db).hasSize(2);
        assertThat(countLeaders()).isOne(); // nur noch uBob Leader

        // 8) restliche Members prüfen (IDs)
        assertThat(db.keySet())
                .extracting(k -> k.getUserId())
                .containsExactlyInAnyOrder(uLeader, uBob);
    }

    private long countLeaders() {
        return db.values().stream().filter(TeamMember::isLeader).count();
    }
}

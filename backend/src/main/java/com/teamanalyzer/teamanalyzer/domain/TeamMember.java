package com.teamanalyzer.teamanalyzer.domain;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Table(name = "team_members")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TeamMember {

    @EmbeddedId
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private TeamMemberKey id = new TeamMemberKey();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("teamId")
    @JoinColumn(name = "team_id", nullable = false, columnDefinition = "BINARY(16)")
    @Setter(AccessLevel.PACKAGE)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    @Setter(AccessLevel.PACKAGE)
    private User user;

    @Column(nullable = false)
    @Setter
    private boolean leader = false;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TeamMember() {
    }

    public static TeamMember of(Team team, User user, boolean leader) {
        TeamMember tm = new TeamMember();
        tm.team = team;
        tm.user = user;
        tm.leader = leader;
        tm.id = new TeamMemberKey(team.getId(), user.getId());
        return tm;
    }

    @PrePersist
    void ensureId() {
        if (id == null)
            id = new TeamMemberKey();
        if (id.getTeamId() == null && team != null)
            id.setTeamId(team.getId());
        if (id.getUserId() == null && user != null)
            id.setUserId(user.getId());
    }
}

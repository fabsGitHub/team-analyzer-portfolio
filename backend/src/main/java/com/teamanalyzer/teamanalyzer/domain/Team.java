package com.teamanalyzer.teamanalyzer.domain;

import java.util.*;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "teams")
public class Team extends UuidEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<TeamMember> members = new ArrayList<>();

    protected Team() {
    }

    public Team(String name) {
        this.name = name;
    }

    public void addMember(TeamMember member) {
        members.add(member);
        member.setTeam(this);
    }

    public void removeMember(TeamMember member) {
        members.remove(member);
        member.setTeam(null);
    }

    public static Team ref(UUID id) {
        Team t = new Team();
        t.setId(id);
        return t;
    }

    public void setName(String name) {
        this.name = name;
    }
}

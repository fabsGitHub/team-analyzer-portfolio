// backend/src/main/java/com/teamanalyzer/teamanalyzer/domain/Survey.java
package com.teamanalyzer.teamanalyzer.domain;

import java.util.*;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "surveys")
public class Survey extends UuidEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Team team;

    @Column(name = "created_by", columnDefinition = "BINARY(16)", nullable = false)
    private java.util.UUID createdBy;

    @Column(nullable = false, length = 300)
    private String title;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idx ASC")
    private final List<SurveyQuestion> questions = new ArrayList<>();

    protected Survey() {
    }

    public void addQuestion(SurveyQuestion q) {
        questions.add(q);
        q.setSurvey(this);
    }

    public void removeQuestion(SurveyQuestion q) {
        questions.remove(q);
        q.setSurvey(null);
    }

    public static Survey create(Team team, UUID createdBy, String title) {
        Survey s = new Survey();
        s.team = team;
        s.createdBy = createdBy;
        s.title = title;
        return s;
    }

    // nur ID-Referenz ohne DB-Load
    public static Survey ref(UUID id) {
        Survey s = new Survey();
        s.setId(id);
        return s;
    }

    // gezielte Mutatoren, die Services verwenden
    public void setTeam(Team team) {
        this.team = team;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

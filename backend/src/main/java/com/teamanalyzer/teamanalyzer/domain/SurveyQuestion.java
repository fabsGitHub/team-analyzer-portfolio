package com.teamanalyzer.teamanalyzer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "survey_questions")
public class SurveyQuestion extends UuidEntity {

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Survey survey;

  @Column(nullable = false)
  private short idx; // 1..n

  @Column(nullable = false, length = 1000)
  private String text;
}

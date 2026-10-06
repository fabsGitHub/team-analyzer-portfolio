package com.teamanalyzer.teamanalyzer.domain;

import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "survey_tokens", indexes = {
    @Index(name = "ix_st_redeemed", columnList = "redeemed"),
    @Index(name = "ix_st_revoked", columnList = "revoked"),
    @Index(name = "ix_st_issued_at", columnList = "issued_at")
})
public class SurveyToken extends UuidEntity {

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Survey survey;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "token_hash", nullable = false, columnDefinition = "BINARY(32)")
  private byte[] tokenHash; // SHA-256

  @Column(name = "issued_to_email", length = 254)
  private String issuedToEmail;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "issued_to_user_id", columnDefinition = "BINARY(16)")
  private User issuedToUser;

  private Instant issuedAt;
  private Instant redeemedAt;

  @Column(nullable = false)
  private boolean redeemed = false;

  @Column(nullable = false)
  private boolean revoked = false;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public void redeemNow(Instant now) {
    this.redeemed = true;
    this.redeemedAt = now;
  }

  public void revokeNow(Instant now) {
    this.revoked = true;
    this.revokedAt = now;
  }
}

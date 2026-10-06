package com.teamanalyzer.teamanalyzer.domain;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

@Validated
@Getter
@ToString(exclude = "hmacSecret")
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

  @NotBlank
  private final String issuer;

  @NotNull
  private final Duration emailVerifyTtl; // z.B. "PT30M" oder "30m"

  @NotBlank
  private final String hmacSecret;

  public AuthProperties(String issuer, Duration emailVerifyTtl, String hmacSecret) {
    this.issuer = issuer;
    this.emailVerifyTtl = emailVerifyTtl;
    this.hmacSecret = hmacSecret;
  }

  // >>> sicherstellen, dass dieser Name existiert
  public long getEmailVerifyExpMin() {
    return emailVerifyTtl.toMinutes();
  }
}

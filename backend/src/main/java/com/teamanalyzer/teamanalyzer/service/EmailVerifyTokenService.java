package com.teamanalyzer.teamanalyzer.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.teamanalyzer.teamanalyzer.domain.AuthProperties;
import com.teamanalyzer.teamanalyzer.port.AppClock;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

import java.time.Instant;
import java.util.Date;

@Service
public class EmailVerifyTokenService {
  /** HS512 requires a key of at least 512 bits. */
  private static final int MIN_SECRET_BYTES = 64;

  private final byte[] secret;
  private final String issuer;
  private final long expMinutes;
  private final AppClock clock;

  public EmailVerifyTokenService(AuthProperties props, AppClock clock) {
    this.secret = props.getHmacSecret().getBytes(StandardCharsets.UTF_8);
    if (secret.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException("app.auth.hmac-secret (EMAIL_VERIFY_HMAC_SECRET) must contain at least "
          + MIN_SECRET_BYTES + " bytes, e.g. the output of `openssl rand -hex 32`");
    }
    this.issuer = props.getIssuer();
    this.expMinutes = props.getEmailVerifyExpMin();
    this.clock = clock;
  }

  /**
   * Creates a confirmation token. It proves control of the mailbox only; confirming the
   * account additionally requires the account's password (see AuthController#verify).
   */
  public String create(String email, Instant now) {
    Instant exp = now.plusSeconds(expMinutes * 60);

    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(email)
        .issueTime(Date.from(now))
        .expirationTime(Date.from(exp))
        .claim("type", "email-verify")
        .build();

    try {
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
      jwt.sign(new MACSigner(secret));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Failed to sign verification token", e);
    }
  }

  /** @return the email address the token was issued for */
  public String validateAndGetEmail(String token) {
    try {
      SignedJWT jwt = SignedJWT.parse(token);
      if (!jwt.verify(new MACVerifier(secret))) {
        throw new IllegalArgumentException("Invalid signature");
      }
      var c = jwt.getJWTClaimsSet();
      if (!issuer.equals(c.getIssuer()))
        throw new IllegalArgumentException("Wrong issuer");
      if (!"email-verify".equals(c.getStringClaim("type")))
        throw new IllegalArgumentException("Wrong token type");
      if (c.getExpirationTime() == null || c.getExpirationTime().toInstant().isBefore(clock.now()))
        throw new IllegalArgumentException("Token expired");
      return c.getSubject();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid token", e);
    }
  }
}

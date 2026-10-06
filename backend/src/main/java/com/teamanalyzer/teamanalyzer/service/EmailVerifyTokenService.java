package com.teamanalyzer.teamanalyzer.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.teamanalyzer.teamanalyzer.domain.AuthProperties;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class EmailVerifyTokenService {
  private final byte[] secret;
  private final String issuer;
  private final long expMinutes;

  public EmailVerifyTokenService(AuthProperties props) {
    this.secret = props.getHmacSecret().getBytes(StandardCharsets.UTF_8);
    this.issuer = props.getIssuer();
    this.expMinutes = props.getEmailVerifyExpMin();
  }

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
      if (c.getExpirationTime() == null || c.getExpirationTime().before(new Date()))
        throw new IllegalArgumentException("Token expired");
      return c.getSubject();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid token", e);
    }
  }
}

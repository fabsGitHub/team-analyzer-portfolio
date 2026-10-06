package com.teamanalyzer.teamanalyzer.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.port.AppClock;

@Component
public class JwtService {

    private final byte[] key;
    private final String issuer;
    private final long ttlMinutes;
    private final JWSAlgorithm alg;
    private final JWSHeader header;
    private final AppClock clock; 

    public JwtService(
            @Value("${app.jwt.secret}") String secretBase64,
            @Value("${app.auth.issuer}") String issuer,
            @Value("${app.jwt.ttl-minutes:60}") long ttlMinutes,
            AppClock clock) {

        this.key = Base64.getDecoder().decode(secretBase64 == null ? "" : secretBase64);
        if (this.key.length >= 64) {
            this.alg = JWSAlgorithm.HS512;
        } else if (this.key.length >= 32) {
            this.alg = JWSAlgorithm.HS256;
        } else {
            throw new IllegalStateException(
                    "app.jwt.secret (Base64) ist zu kurz: mindestens 256 Bit (32 Bytes) erforderlich");
        }
        this.header = new JWSHeader(this.alg);
        this.issuer = issuer;
        this.ttlMinutes = ttlMinutes;
        this.clock = clock; 
    }

    public String createAccessToken(User u) {
        List<String> roles = (u.getRoles() == null)
                ? List.of()
                : u.getRoles().stream()
                        .map(r -> String.valueOf(r.name()))
                        .collect(Collectors.toList());

        Instant now = clock.now();
        Instant expI = now.plus(ttlMinutes, ChronoUnit.MINUTES);

        var claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(u.getEmail())
                .claim("uid", u.getId().toString())
                .claim("email", u.getEmail())
                .claim("roles", roles)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expI))
                .build();

        var jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(new MACSigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign access token", e);
        }
    }

    public JWTClaimsSet validate(String token) {
        try {
            var jwt = SignedJWT.parse(token);
            var verifier = new MACVerifier(key);
            if (!jwt.verify(verifier)) {
                throw new BadCredentialsException("Invalid signature");
            }
            var claims = jwt.getJWTClaimsSet();
            var exp = claims.getExpirationTime();

            // Clock hier verwenden
            if (exp == null || clock.now().isAfter(exp.toInstant())) {
                throw new CredentialsExpiredException("Expired");
            }

            return claims;
        } catch (BadCredentialsException | CredentialsExpiredException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid token", e);
        }
    }
}

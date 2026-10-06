package com.teamanalyzer.teamanalyzer.domain;

import java.time.Instant;
import java.util.Base64;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "ix_rt_user_revoked", columnList = "user_id, revoked"),
        @Index(name = "ix_rt_revoked_expires", columnList = "revoked, expires_at"),
        @Index(name = "ix_rt_expires_at", columnList = "expires_at")
})
public class RefreshToken extends UuidEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    @Setter(AccessLevel.PACKAGE)
    private User user;

    // BINARY(32) für SHA-256
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "BINARY(32)")
    private byte[] tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;


    protected RefreshToken() {
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }

    public void setTokenHash(String h) {
        this.tokenHash = decodeTokenHash(h);
    }

    public void setExpiresAt(Instant t) {
        this.expiresAt = t;
    }

    public void setUserAgent(String ua) {
        this.userAgent = ua;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public User getUser() {
        return user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIp() {
        return ip;
    }

    public static RefreshToken create(User u, String hash, Instant exp, String ua, String ip) {
        RefreshToken rt = new RefreshToken();
        rt.user = u;
        rt.tokenHash = decodeTokenHash(hash);
        rt.expiresAt = exp;
        rt.userAgent = ua;
        rt.ip = ip;
        return rt;
    }

    private static byte[] decodeTokenHash(String h) {
        return Base64.getUrlDecoder().decode(h);
    }
}

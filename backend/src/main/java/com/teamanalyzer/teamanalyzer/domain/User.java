package com.teamanalyzer.teamanalyzer.domain;

import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.BatchSize;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Getter
@Table(name = "users", indexes = {
        @Index(name = "ix_users_email", columnList = "email", unique = true),
        @Index(name = "ix_users_created_at", columnList = "created_at"),
        @Index(name = "ix_users_reset_token", columnList = "reset_token")
})
public class User extends UuidEntity {

    @Email
    @NotBlank
    @Column(name = "email", nullable = false, length = 254, unique = true)
    private String email;

    @ToString.Exclude
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private final Set<RefreshToken> refreshTokens = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), uniqueConstraints = @UniqueConstraint(columnNames = {
            "user_id", "role" }))
    @Column(name = "role", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private final Set<Role> roles = new HashSet<>(Set.of(Role.USER));

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    // --- Password Reset ---
    @Column(name = "reset_token", length = 100, unique = true)
    private String resetToken;

    @Column(name = "reset_token_created")
    private Instant resetTokenCreated;

    protected User() {
        /* for JPA */ }

    private User(String email, String passwordHash) { // ← ctor bleibt package/private
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /**
     * Convenience-Factory: erstellt einen normalisierten User mit bereits gehashtem
     * Passwort.
     */
    public static User of(String email, String passwordHash) {
        return new User(email, passwordHash);
    }

    // Intention-revealing Helpers
    public void verifyEmailNow(Instant now) {
        this.enabled = true;
        this.emailVerifiedAt = now;
    }

    public void setResetToken(String token, Instant created) {
        this.resetToken = token;
        this.resetTokenCreated = created;
    }

    public void clearResetToken() {
        this.resetToken = null;
        this.resetTokenCreated = null;
    }

    public void addRefreshToken(RefreshToken token) {
        refreshTokens.add(token);
        token.setUser(this);
    }

    public void removeRefreshToken(RefreshToken token) {
        refreshTokens.remove(token);
        token.setUser(null);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (email != null)
            email = normalizeEmail(email);
    }

    private static String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String hash) {
        this.passwordHash = hash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEmailVerifiedAt(Instant at) {
        this.emailVerifiedAt = at;
    }

    public void setResetToken(String token) {
        this.resetToken = token;
    }

    public void setResetTokenCreated(Instant created) {
        this.resetTokenCreated = created;
    }

    public Instant getResetTokenCreated() {
        return resetTokenCreated;
    }
}

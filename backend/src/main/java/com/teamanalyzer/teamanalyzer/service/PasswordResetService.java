package com.teamanalyzer.teamanalyzer.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.port.AppClock;
import com.teamanalyzer.teamanalyzer.port.DigestService;
import com.teamanalyzer.teamanalyzer.port.EmailSender;
import com.teamanalyzer.teamanalyzer.port.PasswordHasher;
import com.teamanalyzer.teamanalyzer.repo.RefreshTokenRepository;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;

/**
 * Password-reset flow. Only a SHA-256 hash of the one-time token is stored, so a database
 * leak does not expose usable reset links. A successful reset revokes all refresh tokens of the user.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokens;
    private final EmailSender emailSender;
    private final PasswordHasher passwordHasher;
    private final DigestService digest;
    private final AppClock clock;
    private final AccountVerificationService accountVerification;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.auth.log-verification-link:false}")
    private boolean logLinks;

    @Value("${app.password-reset.ttl-hours:1}")
    private long ttlHours;

    public PasswordResetService(UserRepository userRepository, RefreshTokenRepository refreshTokens,
            EmailSender emailSender, PasswordHasher passwordHasher, DigestService digest, AppClock clock,
            AccountVerificationService accountVerification) {
        this.userRepository = userRepository;
        this.refreshTokens = refreshTokens;
        this.emailSender = emailSender;
        this.passwordHasher = passwordHasher;
        this.digest = digest;
        this.clock = clock;
        this.accountVerification = accountVerification;
    }

    /**
     * Not transactional on purpose: the token is saved (and committed) first, then the mail is
     * sent, so a slow mail provider never holds a database connection. Delivery failures are
     * logged and not reported, so the endpoint answers the same for every address.
     */
    public void sendResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(User.normalizeEmail(email));
        if (userOpt.isEmpty()) {
            return; // do not reveal whether the address is registered
        }

        User user = userOpt.get();
        String token = newToken();
        user.setResetToken(hash(token), clock.now());
        userRepository.save(user);

        String link = frontendBaseUrl.replaceAll("/+$", "") + "/auth/reset?token=" + token;
        if (!mailEnabled && logLinks) {
            log.warn("LOCAL ONLY: password reset link for {} -> {}", email, link);
        }

        String subject = "Reset your Team Analyzer password";
        String body = """
                Hello,

                someone asked to reset the password for your Team Analyzer account.
                To choose a new password, open this link within %d hour(s):
                %s

                If you did not request this, you can ignore this email.
                """.formatted(ttlHours, link);
        try {
            emailSender.send(user.getEmail(), subject, body);
        } catch (RuntimeException ex) {
            log.warn("Could not send password reset email: {}", ex.toString());
        }
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            return false;
        }
        var userOpt = userRepository.findByResetToken(hash(token));
        if (userOpt.isEmpty()) {
            return false;
        }
        var user = userOpt.get();
        var created = user.getResetTokenCreated();
        if (created == null || clock.now().isAfter(created.plus(Duration.ofHours(ttlHours)))) {
            return false;
        }

        user.setPasswordHash(passwordHasher.hash(newPassword));
        user.clearResetToken();
        userRepository.save(user);
        refreshTokens.revokeAllForUser(user.getId());
        // The reset link proves control of the mailbox and the new password is the owner's,
        // so an unconfirmed account is confirmed now (its old confirmation link would no
        // longer match the password).
        userRepository.findByEmailWithRoles(user.getEmail()).ifPresent(accountVerification::confirm);
        return true;
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        return HexFormat.of().formatHex(digest.sha256(token));
    }
}

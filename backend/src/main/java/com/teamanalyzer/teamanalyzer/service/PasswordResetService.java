// backend/src/main/java/com/teamanalyzer/teamanalyzer/service/PasswordResetService.java
package com.teamanalyzer.teamanalyzer.service;

import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.port.AppClock;
import com.teamanalyzer.teamanalyzer.port.EmailSender;
import com.teamanalyzer.teamanalyzer.port.PasswordHasher;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final PasswordHasher passwordHasher;
    private final AppClock clock;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.password-reset.ttl-hours:1}")
    private long ttlHours;

    public PasswordResetService(UserRepository userRepository, EmailSender emailSender,
            PasswordHasher passwordHasher, AppClock clock) {
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Transactional
    public void sendResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty())
            return; // privacy

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token, clock.now());
        userRepository.save(user);

        String link = frontendBaseUrl + "/auth/reset?token=" + token;
        if (log.isDebugEnabled())
            log.debug("DEV: Password reset link for {} -> {}", email, link);

        if (mailEnabled) {
            String subject = "Password Reset for Team Analyzer";
            String body = "Hello,\n\nTo reset your password, click:\n" + link +
                    "\n\nIf you did not request this, you can ignore this email.";
            emailSender.send(user.getEmail(), subject, body);
        }
    }

    @Transactional(readOnly = true)
    public boolean verifyResetToken(String token) {
        var userOpt = userRepository.findByResetToken(token);
        if (userOpt.isEmpty())
            return false;
        var created = userOpt.get().getResetTokenCreated();
        if (created == null)
            return false;
        return !clock.now().isAfter(created.plus(Duration.ofHours(ttlHours)));
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        var userOpt = userRepository.findByResetToken(token);
        if (userOpt.isEmpty())
            return false;
        var user = userOpt.get();
        if (!verifyResetToken(token))
            return false;

        user.setPasswordHash(passwordHasher.hash(newPassword));
        user.setResetToken(null, null);
        userRepository.save(user);
        return true;
    }
}

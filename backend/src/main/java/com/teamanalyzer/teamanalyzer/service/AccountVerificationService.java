package com.teamanalyzer.teamanalyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.Role;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.port.AppClock;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;

/**
 * Marks an account as confirmed once someone has proven both control of the mailbox and
 * knowledge of the account's password (confirmation link + password, or a completed password
 * reset). Only then may the first-admin bootstrap address receive the ADMIN role, so a stranger
 * who registered that address cannot have their password activated by the owner's click.
 */
@Service
public class AccountVerificationService {

    private final UserRepository users;
    private final AppClock clock;
    private final String bootstrapAdminEmail;

    public AccountVerificationService(UserRepository users, AppClock clock,
            @Value("${app.auth.bootstrap-admin-email:}") String bootstrapAdminEmail) {
        this.users = users;
        this.clock = clock;
        this.bootstrapAdminEmail = bootstrapAdminEmail == null || bootstrapAdminEmail.isBlank()
                ? ""
                : User.normalizeEmail(bootstrapAdminEmail);
    }

    /** @param user an account loaded with its roles */
    @Transactional
    public void confirm(User user) {
        boolean changed = false;
        if (!user.isEnabled()) {
            user.verifyEmailNow(clock.now());
            changed = true;
        }
        if (!bootstrapAdminEmail.isEmpty()
                // Exact match on the canonical form: equalsIgnoreCase would also match other
                // addresses such as "krıstin@" (dotless i) or "kriſtin@" (long s).
                && user.getEmail().equals(bootstrapAdminEmail)
                && user.getRoles().add(Role.ADMIN)) {
            changed = true;
        }
        if (changed) {
            users.save(user);
        }
    }
}

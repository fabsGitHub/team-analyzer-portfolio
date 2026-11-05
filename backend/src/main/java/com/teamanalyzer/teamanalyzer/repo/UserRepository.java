// backend/src/main/java/com/teamanalyzer/teamanalyzer/repo/UserRepository.java
package com.teamanalyzer.teamanalyzer.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.User;

@Repository
@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    /**
     * Eager-Laden der Rollen (ElementCollection) zur Vermeidung von N+1
     * in Security-Kontexten (Authentication/Authorization).
     */

    boolean existsByEmail(String email);

    /** Whether the account's email matches a SQL LIKE pattern, e.g. {@code DemoAccounts.EMAIL_LIKE}. */
    boolean existsByIdAndEmailLike(UUID id, String emailPattern);

    /** Role check without loading the (lazy) role collection. */
    boolean existsByIdAndRolesContaining(UUID id, com.teamanalyzer.teamanalyzer.domain.Role role);

    // --- Ergänzungen für PasswordResetService ---
    Optional<User> findByResetToken(String resetToken);

    @EntityGraph(attributePaths = "roles")
    // Exact match: emails are stored in canonical form (User.normalizeEmail). SQL lower()/upper()
    // would merge distinct addresses (e.g. i and dotless ı) and could return the wrong account.
    @Query("select u from User u where u.email = ?1")
    Optional<User> findByEmailWithRoles(String email);
}

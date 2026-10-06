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
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    // --- Ergänzungen für PasswordResetService ---
    Optional<User> findByResetToken(String resetToken);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from User u where lower(u.email) = lower(?1)")
    Optional<User> findByEmailWithRoles(String email);
}

// backend/src/main/java/com/teamanalyzer/teamanalyzer/repo/TeamMemberRepository.java
package com.teamanalyzer.teamanalyzer.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.TeamMember;
import com.teamanalyzer.teamanalyzer.domain.TeamMemberKey;

@Repository
@Transactional(readOnly = true)
public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberKey> {

    /** Alle Mitglieder eines Teams. */
    List<TeamMember> findByTeam_Id(UUID teamId);

    /** Gibt es Leader in einem Team? */
    boolean existsByTeam_IdAndLeaderTrue(UUID teamId);

    /** Anzahl Leader (z. B. für Invariantenprüfung in der Domäne). */
    long countByTeam_IdAndLeaderTrue(UUID teamId);

    boolean existsByTeam_IdAndUser_Id(UUID teamId, UUID userId);

    /** Ist bestimmtes Mitglied Leader? */
    boolean existsByTeam_IdAndUser_IdAndLeaderTrue(UUID teamId, UUID userId);

    /** Hat User irgendwo Leader-Rolle? */
    boolean existsByUser_IdAndLeaderTrue(UUID userId);

    /** Entfernen — Side-Effect, daher explizit @Transactional. */
    @Transactional
    void deleteByTeam_IdAndUser_Id(UUID teamId, UUID userId);

    @Transactional
    void deleteByTeam_Id(UUID teamId);
}

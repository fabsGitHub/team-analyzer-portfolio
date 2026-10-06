// backend/src/main/java/com/teamanalyzer/teamanalyzer/repo/TeamRepository.java
package com.teamanalyzer.teamanalyzer.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.Team;

@Repository
@Transactional(readOnly = true)
public interface TeamRepository extends JpaRepository<Team, UUID> {

    /**
     * Alle Teams inkl. Mitglieder + zugehörige User, um N+1 zu vermeiden
     * (bewusst auf findAll beschränkt—große Tabellen nur mit Bedacht verwenden).
     */
    @Override
    @EntityGraph(attributePaths = { "members", "members.user" })
    List<Team> findAll();

    /** Leichte Projektion aller Teams. */
    List<TeamLiteView> findAllProjectedBy();

    /** Teams, in denen der User Mitglied ist. */
    List<TeamLiteView> findDistinctByMembers_User_Id(UUID userId);

    /** Teams, in denen der User Leader ist. */
    List<TeamLiteView> findDistinctByMembers_User_IdAndMembers_LeaderTrue(UUID userId);
}

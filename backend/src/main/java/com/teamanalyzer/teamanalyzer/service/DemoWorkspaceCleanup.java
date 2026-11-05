package com.teamanalyzer.teamanalyzer.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.DemoAccounts;

import lombok.RequiredArgsConstructor;

/**
 * Deletes expired demo workspaces. Runs whether or not demo mode is on, so workspaces created
 * before the demo was switched off are still removed after 24 hours.
 */
@Service
@RequiredArgsConstructor
public class DemoWorkspaceCleanup {
  /**
   * Workspaces expire 24 hours after their account was created. The cutoff is computed by the
   * database, in the same time zone as the TIMESTAMP column, so the JVM's zone cannot skew it.
   */
  private static final String EXPIRED = "created_at < CURRENT_TIMESTAMP - INTERVAL 24 HOUR";

  private final JdbcTemplate jdbc;

  /**
   * Deletes demo accounts older than 24 hours together with their teams (surveys, tokens,
   * responses and answers cascade). Only teams without any non-demo member are deleted, so a
   * demo account that was added to a real team never takes that team with it.
   */
  @Scheduled(fixedDelayString = "${app.demo.cleanup-interval-ms:3600000}")
  @Transactional
  public void cleanupExpiredWorkspaces() {
    jdbc.update("""
        DELETE FROM teams
         WHERE id IN (
           SELECT team_id FROM (
             SELECT tm.team_id
               FROM team_members tm
               JOIN users u ON u.id = tm.user_id
              WHERE u.email LIKE ?
                AND u.%s
                AND NOT EXISTS (
                  SELECT 1
                    FROM team_members other
                    JOIN users ou ON ou.id = other.user_id
                   WHERE other.team_id = tm.team_id
                     AND ou.email NOT LIKE ?)
           ) expired_demo_teams
         )
        """.formatted(EXPIRED), DemoAccounts.EMAIL_LIKE, DemoAccounts.EMAIL_LIKE);
    // A demo account that still owns a survey in a kept team stays until that survey is gone.
    jdbc.update("""
        DELETE FROM users
         WHERE email LIKE ?
           AND %s
           AND NOT EXISTS (SELECT 1 FROM surveys s WHERE s.created_by = users.id)
        """.formatted(EXPIRED), DemoAccounts.EMAIL_LIKE);
  }
}

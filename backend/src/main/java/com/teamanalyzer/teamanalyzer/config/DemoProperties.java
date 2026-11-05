package com.teamanalyzer.teamanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

/**
 * Demo mode ({@code app.demo.*}) and the limits for anonymous demo visitors. The values are set
 * in {@code application.yml}; a missing or non-positive limit stops the application at startup.
 * Loaded whether or not demo mode is on, because existing demo accounts must also be handled
 * while it is off.
 *
 * @param enabled                      whether {@code POST /api/demo/session} exists and demo
 *                                     accounts may sign in
 * @param maxSessionsPerAddressPerHour demo starts per client address and hour, per API instance
 * @param trackedAddresses             client addresses the per-address limit keeps in memory
 * @param maxSessionsPerHour           demo starts per hour across all instances
 * @param maxLiveWorkspaces            demo workspaces that may exist at the same time
 * @param maxSurveysPerWorkspace       surveys in one demo team, the seeded one included
 * @param maxTokensPerSurvey           participation tokens per demo survey, renewed ones included
 */
@Validated
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(
    boolean enabled,
    @Positive int maxSessionsPerAddressPerHour,
    @Positive int trackedAddresses,
    @Positive int maxSessionsPerHour,
    @Positive int maxLiveWorkspaces,
    @Positive int maxSurveysPerWorkspace,
    @Positive int maxTokensPerSurvey) {
}

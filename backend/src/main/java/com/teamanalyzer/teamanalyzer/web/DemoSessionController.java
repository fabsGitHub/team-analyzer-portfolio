package com.teamanalyzer.teamanalyzer.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import com.teamanalyzer.teamanalyzer.config.DemoProperties;
import com.teamanalyzer.teamanalyzer.domain.RefreshToken;
import com.teamanalyzer.teamanalyzer.port.AppClock;
import com.teamanalyzer.teamanalyzer.repo.RefreshTokenRepository;
import com.teamanalyzer.teamanalyzer.service.DemoLimitExceededException;
import com.teamanalyzer.teamanalyzer.service.DemoLimitExceededException.Limit;
import com.teamanalyzer.teamanalyzer.service.DemoWorkspaceCleanup;
import com.teamanalyzer.teamanalyzer.service.DemoWorkspaceService;
import com.teamanalyzer.teamanalyzer.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoSessionController {
  private static final Logger log = LoggerFactory.getLogger(DemoSessionController.class);
  private static final Duration REFRESH_TTL = Duration.ofHours(24);
  private static final Duration RATE_WINDOW = Duration.ofHours(1);

  private final DemoWorkspaceService workspaces;
  private final DemoWorkspaceCleanup cleanup;
  private final JwtService jwt;
  private final RefreshTokenRepository refreshTokens;
  private final AppClock clock;
  private final int maxStartsPerAddress;
  private final DemoStartLimiter startsPerAddress;

  @Value("${app.cookies.secure:true}")
  private boolean cookieSecure;

  public DemoSessionController(
      DemoWorkspaceService workspaces,
      DemoWorkspaceCleanup cleanup,
      JwtService jwt,
      RefreshTokenRepository refreshTokens,
      AppClock clock,
      DemoProperties demo) {
    this.workspaces = workspaces;
    this.cleanup = cleanup;
    this.jwt = jwt;
    this.refreshTokens = refreshTokens;
    this.clock = clock;
    this.maxStartsPerAddress = demo.maxSessionsPerAddressPerHour();
    this.startsPerAddress = new DemoStartLimiter(maxStartsPerAddress, RATE_WINDOW, demo.trackedAddresses());
  }

  @PostMapping("/session")
  public ResponseEntity<DemoSessionResponse> createSession(
      HttpServletRequest request,
      HttpServletResponse response) {
    startsPerAddress.tryAcquire(clientAddress(request), clock.now()).ifPresent(retryAfter -> {
      throw new DemoLimitExceededException(Limit.ADDRESS, maxStartsPerAddress, retryAfter);
    });

    // Serverless containers may sleep through the scheduled cleanup, so expired workspaces are
    // also removed here, in their own transaction: a failure must not block the new demo.
    try {
      cleanup.cleanupExpiredWorkspaces();
    } catch (RuntimeException ex) {
      log.warn("Could not delete expired demo workspaces: {}", ex.toString());
    }

    // Fails with 429 when the global limits (live workspaces, starts per hour) are reached.
    DemoWorkspaceService.DemoWorkspace workspace = workspaces.createWorkspace();
    String accessToken = jwt.createAccessToken(workspace.user());

    String refreshPlain = UUID.randomUUID() + "." + UUID.randomUUID();
    refreshTokens.save(RefreshToken.create(
        workspace.user(),
        sha256Base64Url(refreshPlain),
        clock.now().plus(REFRESH_TTL),
        request.getHeader("User-Agent"),
        request.getRemoteAddr()));

    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshPlain)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/auth")
        .maxAge(REFRESH_TTL)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

    return ResponseEntity.ok(new DemoSessionResponse(accessToken, workspace.surveyId()));
  }

  /**
   * The client address for the rate limit, from headers the proxy in front of the API
   * overwrites: Vercel sets {@code x-vercel-forwarded-for}, the nginx container sets
   * {@code X-Real-IP}. The first {@code X-Forwarded-For} entry is not used, because nginx
   * appends to whatever the client sent there.
   */
  private String clientAddress(HttpServletRequest request) {
    for (String header : new String[] { "x-vercel-forwarded-for", "x-real-ip" }) {
      String value = request.getHeader(header);
      if (value != null && !value.isBlank()) {
        return value.split(",")[0].trim();
      }
    }
    return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
  }

  private String sha256Base64Url(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception exception) {
      throw new IllegalStateException("Could not hash the demo refresh token", exception);
    }
  }

  public record DemoSessionResponse(String accessToken, UUID surveyId) {
  }
}

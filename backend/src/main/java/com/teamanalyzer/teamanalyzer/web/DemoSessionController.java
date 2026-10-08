package com.teamanalyzer.teamanalyzer.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.teamanalyzer.teamanalyzer.domain.RefreshToken;
import com.teamanalyzer.teamanalyzer.port.AppClock;
import com.teamanalyzer.teamanalyzer.repo.RefreshTokenRepository;
import com.teamanalyzer.teamanalyzer.service.DemoWorkspaceService;
import com.teamanalyzer.teamanalyzer.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
public class DemoSessionController {
  private static final Duration REFRESH_TTL = Duration.ofHours(24);
  private static final Duration RATE_WINDOW = Duration.ofHours(1);
  private static final int MAX_SESSIONS_PER_IP_PER_HOUR = 20;

  private final DemoWorkspaceService workspaces;
  private final JwtService jwt;
  private final RefreshTokenRepository refreshTokens;
  private final AppClock clock;
  private final Map<String, RateWindow> recentSessions = new ConcurrentHashMap<>();

  @Value("${app.cookies.secure:true}")
  private boolean cookieSecure;

  @PostMapping("/session")
  public ResponseEntity<DemoSessionResponse> createSession(
      HttpServletRequest request,
      HttpServletResponse response) {
    enforceRateLimit(clientAddress(request));

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

  private String clientAddress(HttpServletRequest request) {
    String forwarded = request.getHeader("x-vercel-forwarded-for");
    if (forwarded == null || forwarded.isBlank()) {
      forwarded = request.getHeader("x-forwarded-for");
    }
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
  }

  private synchronized void enforceRateLimit(String address) {
    var now = clock.now();
    var cutoff = now.minus(RATE_WINDOW);

    if (recentSessions.size() > 1000) {
      recentSessions.entrySet().removeIf(entry -> entry.getValue().lastSeen.isBefore(cutoff));
    }

    if (recentSessions.size() >= 1000 && !recentSessions.containsKey(address)) {
      address = "overflow";
    }
    RateWindow window = recentSessions.computeIfAbsent(address, ignored -> new RateWindow());
    while (!window.requests.isEmpty() && window.requests.peekFirst().isBefore(cutoff)) {
      window.requests.removeFirst();
    }
    if (window.requests.size() >= MAX_SESSIONS_PER_IP_PER_HOUR) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demo session limit reached");
    }
    window.requests.addLast(now);
    window.lastSeen = now;
  }

  private String sha256Base64Url(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception exception) {
      throw new IllegalStateException("Could not hash the demo refresh token", exception);
    }
  }

  private static final class RateWindow {
    private final ArrayDeque<java.time.Instant> requests = new ArrayDeque<>();
    private java.time.Instant lastSeen = java.time.Instant.EPOCH;
  }

  public record DemoSessionResponse(String accessToken, UUID surveyId) {
  }
}

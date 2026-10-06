package com.teamanalyzer.teamanalyzer.web;

import com.teamanalyzer.teamanalyzer.domain.RefreshToken;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.port.AppClock;
import com.teamanalyzer.teamanalyzer.repo.RefreshTokenRepository;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import com.teamanalyzer.teamanalyzer.service.EmailVerifyTokenService;
import com.teamanalyzer.teamanalyzer.service.JwtService;
import com.teamanalyzer.teamanalyzer.service.MailService;
import com.teamanalyzer.teamanalyzer.service.PasswordResetService;
import com.teamanalyzer.teamanalyzer.web.dto.ConfirmPasswordDto;
import com.teamanalyzer.teamanalyzer.web.dto.LoginDto;
import com.teamanalyzer.teamanalyzer.web.dto.RegisterDto;
import com.teamanalyzer.teamanalyzer.web.dto.ResetPasswordDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository users;
    private final RefreshTokenRepository tokens;
    private final PasswordEncoder enc;
    private final JwtService jwt;
    private final EmailVerifyTokenService emailTokenSvc;
    private final MailService mail;
    private final PasswordResetService passwordResetService;
    private final AppClock clock;

    private final String frontendBaseUrl;
    private final String verifyEndpointPath;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.cookies.secure:true}")
    private boolean cookieSecure;

    public AuthController(
            UserRepository users,
            RefreshTokenRepository tokens,
            PasswordEncoder enc,
            JwtService jwt,
            EmailVerifyTokenService emailTokenSvc,
            MailService mail,
            PasswordResetService passwordResetService,
            @Value("${app.frontend-base-url}") String frontendBaseUrl,
            @Value("${app.verify-endpoint-path}") String verifyEndpointPath,
            AppClock clock) {
        this.users = users;
        this.tokens = tokens;
        this.enc = enc;
        this.jwt = jwt;
        this.emailTokenSvc = emailTokenSvc;
        this.mail = mail;
        this.passwordResetService = passwordResetService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.verifyEndpointPath = verifyEndpointPath;
        this.clock = clock;
    }

    private ResponseCookie buildRefreshCookie(String value, HttpServletRequest req, long maxAgeSeconds) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    // --- Register ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDto dto) {
        String email = dto.email().trim().toLowerCase();

        if (users.existsByEmail(email))
            return ResponseEntity.status(409).build();

        var u = User.of(email, enc.encode(dto.password()));
        u.setEnabled(false);
        users.save(u);

        String token = emailTokenSvc.create(email, clock.now());
        String link = frontendBaseUrl + verifyEndpointPath + "?token=" + token;

        log.info("DEV: Verification link for {} -> {}", email, link);

        if (mailEnabled) {
            try {
                String subject = "Bitte bestätige deine E-Mail-Adresse";
                String body = """
                        Willkommen bei TeamAnalyzer!

                        Bitte bestätige deine E-Mail, indem du auf den folgenden Link klickst:
                        %s

                        Der Link ist zeitlich begrenzt gültig.
                        """.formatted(link);
                mail.send(email, subject, body);
            } catch (Exception ex) {
                log.warn("E-Mail-Versand fehlgeschlagen (fahre ohne Mail fort): {}", ex.toString());
            }
        }

        return ResponseEntity.accepted().build();
    }

    // --- Verify ---
    @PostMapping("/verify")
    public ResponseEntity<Void> verifyPost(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return doVerify(token);
    }

    private ResponseEntity<Void> doVerify(String token) {
        String email = emailTokenSvc.validateAndGetEmail(token);
        var user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!user.isEnabled()) {
            user.setEnabled(true);
            user.setEmailVerifiedAt(clock.now());
            users.save(user);
        }
        return ResponseEntity.noContent().build();
    }

    // --- Login ---
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginDto dto,
            HttpServletRequest req,
            HttpServletResponse res) {
        String email = dto.email().trim().toLowerCase();
        var user = users.findByEmailWithRoles(email).orElseThrow(() -> new BadCredentialsException("x"));
        if (!user.isEnabled() || !enc.matches(dto.password(), user.getPasswordHash()))
            throw new BadCredentialsException("x");

        var access = jwt.createAccessToken(user);

        // Plain refresh token + gehashte, Base64URL-kodierte Speicherung
        var refreshPlain = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        var refreshHashB64 = sha256Base64Url(refreshPlain);

        var rt = RefreshToken.create(
                user,
                refreshHashB64,
                clock.now().plus(14, ChronoUnit.DAYS),
                req.getHeader("User-Agent"),
                req.getRemoteAddr());
        tokens.save(rt);

        res.addHeader(HttpHeaders.SET_COOKIE,
                buildRefreshCookie(refreshPlain, req, Duration.ofDays(14).toSeconds()).toString());
        return ResponseEntity.ok(new TokenResponse(access));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(value = "refresh_token", required = false) String refresh,
            HttpServletRequest req,
            HttpServletResponse res) {

        if (refresh == null || refresh.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        var hashB64 = sha256Base64Url(refresh);
        var rt = tokens.findActiveByHashWithUserAndRoles(hashB64)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (rt.getExpiresAt().isBefore(clock.now()) || rt.isRevoked())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        rt.setRevoked(true);
        tokens.save(rt);

        var newPlain = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        var newHashB64 = sha256Base64Url(newPlain);
        tokens.save(RefreshToken.create(rt.getUser(), newHashB64,
                clock.now().plus(14, ChronoUnit.DAYS), rt.getUserAgent(), rt.getIp()));

        var access = jwt.createAccessToken(rt.getUser());
        res.addHeader(HttpHeaders.SET_COOKIE,
                buildRefreshCookie(newPlain, req, Duration.ofDays(14).toSeconds()).toString());

        return ResponseEntity.ok(new TokenResponse(access));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("refresh_token") String refresh,
            HttpServletRequest req,
            HttpServletResponse res) {
        var hashB64 = sha256Base64Url(refresh);
        tokens.revokeByHash(hashB64);
        res.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie("", req, 0).toString());
        return ResponseEntity.noContent().build();
    }

    // --- Password Reset: Request Token ---
    @PostMapping("/reset")
    public ResponseEntity<?> sendResetToken(@Valid @RequestBody ResetPasswordDto dto) {
        passwordResetService.sendResetToken(dto.email().trim().toLowerCase());
        // Immer OK zurückgeben, damit niemand E-Mail-Adressen prüfen kann
        return ResponseEntity.ok().build();
    }

    // --- Password Reset: Set New Password ---

    @PostMapping("/reset/confirm")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ConfirmPasswordDto dto) {
        boolean ok = passwordResetService.resetPassword(dto.token(), dto.newPassword());
        return ok ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token.");
    }

    // SHA-256 -> Base64URL (ohne Padding) als String
    private static String sha256Base64Url(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    record TokenResponse(String accessToken) {
    }
}

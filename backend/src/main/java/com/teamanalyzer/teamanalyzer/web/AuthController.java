package com.teamanalyzer.teamanalyzer.web;

import com.teamanalyzer.teamanalyzer.config.DemoProperties;
import com.teamanalyzer.teamanalyzer.domain.DemoAccounts;
import com.teamanalyzer.teamanalyzer.domain.RefreshToken;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.port.AppClock;
import com.teamanalyzer.teamanalyzer.repo.RefreshTokenRepository;
import com.teamanalyzer.teamanalyzer.repo.UserRepository;
import com.teamanalyzer.teamanalyzer.service.AccountVerificationService;
import com.teamanalyzer.teamanalyzer.service.EmailVerifyTokenService;
import com.teamanalyzer.teamanalyzer.service.JwtService;
import com.teamanalyzer.teamanalyzer.service.MailService;
import com.teamanalyzer.teamanalyzer.service.PasswordResetService;
import com.teamanalyzer.teamanalyzer.web.dto.ConfirmPasswordDto;
import com.teamanalyzer.teamanalyzer.web.dto.LoginDto;
import com.teamanalyzer.teamanalyzer.web.dto.RegisterDto;
import com.teamanalyzer.teamanalyzer.web.dto.ResetPasswordDto;
import com.teamanalyzer.teamanalyzer.web.dto.VerifyEmailDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String INVALID_LOGIN = "Invalid email or password, or the email address is not confirmed yet.";

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
    private final AccountVerificationService accountVerification;
    private final DemoProperties demo;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.auth.log-verification-link:false}")
    private boolean logVerificationLink;

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
            AccountVerificationService accountVerification,
            @Value("${app.verify-endpoint-path}") String verifyEndpointPath,
            AppClock clock,
            DemoProperties demo) {
        this.users = users;
        this.tokens = tokens;
        this.enc = enc;
        this.jwt = jwt;
        this.emailTokenSvc = emailTokenSvc;
        this.mail = mail;
        this.passwordResetService = passwordResetService;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.accountVerification = accountVerification;
        this.verifyEndpointPath = verifyEndpointPath;
        this.clock = clock;
        this.demo = demo;
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
        String email = User.normalizeEmail(dto.email());
        if (DemoAccounts.isDemoEmail(email)) {
            // Reserved for generated demo workspaces, which are limited and deleted by address.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This email address is reserved.");
        }
        String passwordHash = enc.encode(dto.password());

        var existing = users.findByEmail(email);
        User u;
        if (existing.isPresent()) {
            if (existing.get().isEnabled()) {
                return ResponseEntity.status(409).build();
            }
            // Unconfirmed address: a new registration replaces the password, so nobody can block
            // an address by registering it first. Confirming still requires the mailbox link
            // *and* the current password, so this cannot be used to take over the account.
            u = existing.get();
            u.setPasswordHash(passwordHash);
        } else {
            u = User.of(email, passwordHash);
            u.setEnabled(false);
        }
        try {
            u = users.saveAndFlush(u);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException lostRace) {
            // A concurrent registration of the same address won; it created or updated the
            // pending account and sends its own confirmation mail.
            return ResponseEntity.accepted().build();
        }
        // Mail and token use the stored address, never the raw request string.
        email = u.getEmail();

        String token = emailTokenSvc.create(email, clock.now());
        String link = frontendBaseUrl + verifyEndpointPath + "?token=" + token;

        try {
            String subject = "Confirm your Team Analyzer email address";
            String body = """
                    Welcome to Team Analyzer!

                    Please confirm your email address by opening this link:
                    %s

                    The link expires after a short time. If you did not create an account,
                    you can ignore this email.
                    """.formatted(link);
            mail.send(email, subject, body);
        } catch (Exception ex) {
            // Registration still succeeds; the failure is logged so operators can follow up.
            log.warn("Could not send verification email: {}", ex.toString());
        }

        if (!mailEnabled && logVerificationLink) {
            log.warn("LOCAL ONLY: email verification link for {} -> {}", email, link);
        }

        return ResponseEntity.accepted().build();
    }

    // --- Verify ---
    /**
     * Confirms an address. Requires the link from the mailbox <em>and</em> the account's
     * password: whoever registered the address must also control the mailbox.
     */
    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@Valid @RequestBody VerifyEmailDto body) {
        String email = emailTokenSvc.validateAndGetEmail(body.token());
        var user = users.findByEmailWithRoles(email)
                .filter(u -> enc.matches(body.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid or expired link, or wrong password."));
        accountVerification.confirm(user);
        return ResponseEntity.noContent().build();
    }

    // --- Login ---
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginDto dto,
            HttpServletRequest req,
            HttpServletResponse res) {
        String email = User.normalizeEmail(dto.email());
        var user = users.findByEmailWithRoles(email).orElseThrow(() -> new BadCredentialsException(INVALID_LOGIN));
        if (!user.isEnabled() || !enc.matches(dto.password(), user.getPasswordHash()))
            throw new BadCredentialsException(INVALID_LOGIN);

        var access = jwt.createAccessToken(user);

        // The client gets the plain refresh token; only its SHA-256 hash is stored.
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

        boolean demoAccount = DemoAccounts.isDemoEmail(rt.getUser().getEmail());
        if (demoAccount && !demo.enabled()) {
            // Demo mode was switched off: its sessions end now, not when cleanup deletes them.
            rt.setRevoked(true);
            tokens.save(rt);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        if (demoAccount) {
            // Anyone can use a demo session, so rotating it must not leave a row behind each time.
            tokens.delete(rt);
        } else {
            rt.setRevoked(true);
            tokens.save(rt);
        }

        var newPlain = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        var newHashB64 = sha256Base64Url(newPlain);
        Duration refreshLifetime = refreshLifetimeFor(rt.getUser());
        tokens.save(RefreshToken.create(rt.getUser(), newHashB64,
                clock.now().plus(refreshLifetime), rt.getUserAgent(), rt.getIp()));

        var access = jwt.createAccessToken(rt.getUser());
        res.addHeader(HttpHeaders.SET_COOKIE,
                buildRefreshCookie(newPlain, req, refreshLifetime.toSeconds()).toString());

        return ResponseEntity.ok(new TokenResponse(access));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refresh_token", required = false) String refresh,
            HttpServletRequest req,
            HttpServletResponse res) {
        if (refresh != null && !refresh.isBlank()) {
            tokens.revokeByHash(sha256Base64Url(refresh));
        }
        res.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie("", req, 0).toString());
        return ResponseEntity.noContent().build();
    }

    // --- Password Reset: Request Token ---
    @PostMapping("/reset")
    public ResponseEntity<?> sendResetToken(@Valid @RequestBody ResetPasswordDto dto) {
        passwordResetService.sendResetToken(User.normalizeEmail(dto.email()));
        // Always answer 200 so the endpoint cannot be used to probe for registered addresses.
        return ResponseEntity.ok().build();
    }

    // --- Password Reset: Set New Password ---

    @PostMapping("/reset/confirm")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ConfirmPasswordDto dto) {
        if (!passwordResetService.resetPassword(dto.token(), dto.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token.");
        }
        return ResponseEntity.ok().build();
    }

    /** Demo workspaces are deleted after 24 hours, so their sessions must not outlive them. */
    private static Duration refreshLifetimeFor(User user) {
        return DemoAccounts.isDemoEmail(user.getEmail()) ? Duration.ofHours(24) : Duration.ofDays(14);
    }

    // SHA-256 -> Base64URL (no padding)
    private static String sha256Base64Url(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    record TokenResponse(String accessToken) {
    }
}

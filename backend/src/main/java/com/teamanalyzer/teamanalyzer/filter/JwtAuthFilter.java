// src/main/java/com/teamanalyzer/teamanalyzer/filter/JwtAuthFilter.java
package com.teamanalyzer.teamanalyzer.filter;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nimbusds.jwt.JWTClaimsSet;
import com.teamanalyzer.teamanalyzer.security.AuthUser;
import com.teamanalyzer.teamanalyzer.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // Wenn bereits authentifiziert, Filter hier überspringen (keine Überschreibung)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            chain.doFilter(req, res);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            chain.doFilter(req, res);
            return;
        }

        try {
            JWTClaimsSet claims = jwtService.validate(token);

            String email = extractEmail(claims);
            UUID userId = parseUuidOrNull(readUidAsString(claims));
            List<String> roles = extractRoleNames(claims);

            var authorities = roles.stream()
                    .map(JwtAuthFilter::ensureRolePrefix)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            var principal = new AuthUser(userId, email, roles);
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ParseException pe) {
            // Struktur/Claims nicht lesbar → Debug-Info mit URI
            LOG.debug("JWT parse error for {}: {}", req.getRequestURI(), pe.getMessage());
        } catch (Exception ex) {
            // Signatur/Expiry/sonstige Validierungsfehler
            LOG.debug("Invalid JWT for {}: {}", req.getRequestURI(), ex.getMessage());
        }

        chain.doFilter(req, res);
    }

    private static String extractEmail(JWTClaimsSet claims) throws ParseException {
        String subject = claims.getSubject();
        if (subject != null && !subject.isBlank()) {
            return subject;
        }
        // gezielter Parse; wirft ParseException bei falschem Typ
        String email = claims.getStringClaim(CLAIM_EMAIL);
        return (email != null && !email.isBlank()) ? email : null;
    }

    private static String readUidAsString(JWTClaimsSet claims) throws ParseException {
        // bevorzugt String-Claim; fällt andernfalls auf generischen Claim zurück
        try {
            String uid = claims.getStringClaim(CLAIM_UID);
            if (uid != null)
                return uid;
        } catch (ParseException ignored) {
            // Ignorieren und auf generischen Claim zurückfallen
        }
        Object raw = claims.getClaim(CLAIM_UID);
        return (raw != null) ? String.valueOf(raw) : null;
    }

    private static UUID parseUuidOrNull(String maybeUuid) {
        if (maybeUuid == null || maybeUuid.isBlank())
            return null;
        try {
            return UUID.fromString(maybeUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static List<String> extractRoleNames(JWTClaimsSet claims) {
        Object raw = claims.getClaim(CLAIM_ROLES);
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return Collections.emptyList();
    }

    private static String ensureRolePrefix(String role) {
        if (role == null || role.isBlank())
            return ROLE_PREFIX + "UNKNOWN";
        return role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
    }
}

// backend/src/main/java/com/teamanalyzer/teamanalyzer/config/SecurityConfig.java
package com.teamanalyzer.teamanalyzer.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.teamanalyzer.teamanalyzer.filter.JwtAuthFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  // ---- Single Source of Truth: zentrale Endpunkt-/Rollen-Definitionen ----
  private static final String ROLE_ADMIN = "ADMIN";

  private static final String[] PUBLIC_ENDPOINTS = {
      "/error",
      "/actuator/health",
      "/api/auth/**"
  };

  private static final String[] SURVEY_PUBLIC_ENDPOINTS_GET = {
      "/api/surveys/*",
      "/api/surveys/*/results/download"
  };

  private static final String[] SURVEY_PUBLIC_ENDPOINTS_POST = {
      "/api/surveys/*/responses"
  };

  private static final String[] ADMIN_ENDPOINTS = {
      "/api/admin/**"
  };

  // ---- Konfiguration aus Environment (SSOT, 12-Factor) ----
  @Value("${app.security.bcrypt-strength:12}")
  private int bcryptStrength;

  @Bean
  PasswordEncoder passwordEncoder() {
    // KISS: delegiert nur die Erzeugung, Parametrisierung per Property
    return new BCryptPasswordEncoder(bcryptStrength);
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
    http
        // State & Basis-Setup
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)

        // Autorisierung: klar gruppiert, keine Magic Strings
        .authorizeHttpRequests(auth -> auth
            // Preflight immer erlauben
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // offen
            .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

            // öffentliche Survey-Endpunkte
            .requestMatchers(HttpMethod.GET, SURVEY_PUBLIC_ENDPOINTS_GET).permitAll()
            .requestMatchers(HttpMethod.POST, SURVEY_PUBLIC_ENDPOINTS_POST).permitAll()

            // Admin only
            .requestMatchers(ADMIN_ENDPOINTS).hasRole(ROLE_ADMIN)

            // alles andere erfordert Auth
            .anyRequest().authenticated())

        // JWT vor Username/Passwort-Filter einhängen
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)

        // Einheitliches Fehlerverhalten (keine leeren catches; klare Statuscodes)
        .exceptionHandling(e -> e
            .authenticationEntryPoint((req, res, ex) -> {
              res.setStatus(HttpStatus.UNAUTHORIZED.value());
              res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            })
            .accessDeniedHandler((req, res, ex) -> res.setStatus(HttpStatus.FORBIDDEN.value())));

    return http.build();
  }

  @Bean
  CorsConfigurationSource cors(
      @Value("${app.cors.allowed-origins:http://localhost:5173}") String originsCsv) {

    final List<String> allowedOrigins = Arrays.stream(originsCsv.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();

    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(allowedOrigins);
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of(
        HttpHeaders.AUTHORIZATION,
        HttpHeaders.CONTENT_TYPE,
        "X-Requested-With",
        HttpHeaders.ACCEPT,
        HttpHeaders.ORIGIN));
    cfg.setExposedHeaders(List.of("Content-Disposition")); // für Downloads
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(Duration.ofHours(1).toSeconds()); // lesbarer als nackte Zahl

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }
}

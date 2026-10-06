package com.teamanalyzer.teamanalyzer.web;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamanalyzer.teamanalyzer.domain.Survey;
import com.teamanalyzer.teamanalyzer.repo.SurveyRepository;
import com.teamanalyzer.teamanalyzer.repo.TeamMemberRepository;
import com.teamanalyzer.teamanalyzer.security.AuthUser;
import com.teamanalyzer.teamanalyzer.service.DownloadTokenService;
import com.teamanalyzer.teamanalyzer.service.SurveyService;
import com.teamanalyzer.teamanalyzer.service.TokenService;
import com.teamanalyzer.teamanalyzer.web.dto.CreateSurveyRequestDto;
import com.teamanalyzer.teamanalyzer.web.dto.MyTokenDto;
import com.teamanalyzer.teamanalyzer.web.dto.SubmitSurveyRequestDto;
import com.teamanalyzer.teamanalyzer.web.dto.SurveyDto;
import com.teamanalyzer.teamanalyzer.web.dto.SurveyResultsDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

  private final SurveyService surveyService;
  private final TokenService tokenService;
  private final TeamMemberRepository tmRepo;
  private final SurveyRepository surveyRepo;
  private final DownloadTokenService downloadTokens;
  private final ObjectMapper objectMapper;

  @PersistenceContext
  private EntityManager em;

  @Value("${app.frontend-base-url:}")
  private String frontendBaseUrl;

  private boolean hasRole(String role) {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    return a != null && a.getAuthorities().stream().anyMatch(ga -> role.equals(ga.getAuthority()));
  }

  // --- Survey anlegen (Leader des Teams oder Admin) ---
  @PostMapping
  public ResponseEntity<SurveyDto> create(
      @AuthenticationPrincipal AuthUser me,
      @RequestBody @Validated CreateSurveyRequestDto req) {

    if (req.questions() == null || req.questions().size() != 5)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Require exactly 5 questions");
    if (me == null || me.userId() == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    if (req.teamId() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing teamId");

    boolean isAdmin = hasRole("ROLE_ADMIN");
    boolean isLeaderOfTeam = tmRepo.existsByTeam_IdAndUser_IdAndLeaderTrue(req.teamId(), me.userId());
    if (!(isAdmin || isLeaderOfTeam))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be leader of the selected team");

    var survey = surveyService.createSurvey(me.userId(), req.teamId(), req.title(), req.questions());
    var dto = surveyService.getSurvey(survey.getId());
    return ResponseEntity
        .created(URI.create("/api/surveys/" + survey.getId()))
        .body(dto);
  }

  // --- Fragen lesen (öffentlich) ---
  @GetMapping("/{id}")
  public SurveyDto get(@PathVariable UUID id) {
    return surveyService.getSurvey(id);
  }

  // --- Antworten abgeben (öffentlich mit One-Time-Token) ---
  @PostMapping("/{id}/responses")
  public ResponseEntity<Void> submit(@PathVariable UUID id, @RequestBody @Validated SubmitSurveyRequestDto req) {
    if (req.token() == null || req.token().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing token");
    }
    surveyService.submitAnonymousByPlainToken(
        id,
        req.token(),
        new short[] { req.q1(), req.q2(), req.q3(), req.q4(), req.q5() });
    return ResponseEntity.accepted().build();
  }

  // --- Ergebnisse (nur Admin oder Leader des Survey-Teams) ---
  @GetMapping("/{id}/results")
  public SurveyResultsDto results(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
    if (me == null || me.userId() == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    boolean isAdmin = hasRole("ROLE_ADMIN");
    boolean isLeaderOfTeam = surveyRepo.existsByIdAndTeam_Members_User_IdAndTeam_Members_LeaderTrue(id, me.userId());
    if (!(isAdmin || isLeaderOfTeam)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be leader of this survey's team");
    }
    return surveyService.getResults(id);
  }

  // --- Tokens für alle Teammitglieder des Surveys sicherstellen ---
  @PostMapping("/{id}/tokens/for-members")
  public Map<String, Object> issueForMembers(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
    if (me == null || me.userId() == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    boolean isAdmin = hasRole("ROLE_ADMIN");
    boolean isLeaderOfTeam = surveyRepo.existsByIdAndTeam_Members_User_IdAndTeam_Members_LeaderTrue(id, me.userId());
    if (!(isAdmin || isLeaderOfTeam)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be leader of this survey's team");
    }
    int created = tokenService.ensureTokensForAllTeamMembers(id);
    return Map.of("created", created, "surveyId", id);
  }

  @PutMapping("/{id}/my-token")
  public MyTokenDto ensureMyToken(
      @AuthenticationPrincipal AuthUser me,
      @PathVariable UUID id,
      HttpServletRequest req) {
    if (me == null || me.userId() == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

    UUID teamId = surveyRepo.findTeamIdById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    boolean isMember = tmRepo.existsByTeam_IdAndUser_Id(teamId, me.userId());
    if (!isMember && !hasRole("ROLE_ADMIN")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this survey's team");
    }

    Survey surveyRef = em.getReference(Survey.class, id);

    String plain = tokenService.ensurePersonalToken(surveyRef, me.userId(), me.email());
    boolean created = (plain != null);
    String inviteLink = (plain != null) ? buildInviteLink(req, id, plain) : null;

    return new MyTokenDto(created, inviteLink);
  }

  // --- Mitglied fordert bewusst einen neuen (alte werden revoked) ---
  @PostMapping("/{id}/my-token/renew")
  public MyTokenDto renewMyToken(@AuthenticationPrincipal AuthUser me,
      @PathVariable UUID id, HttpServletRequest req) {
    if (me == null || me.userId() == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

    UUID teamId = surveyRepo.findTeamIdById(id) // <— umbenannte Repo-Methode
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    boolean isMember = tmRepo.existsByTeam_IdAndUser_Id(teamId, me.userId());
    if (!isMember && !hasRole("ROLE_ADMIN")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this survey's team");
    }

    Survey surveyRef = em.getReference(Survey.class, id);
    String plain = tokenService.renewPersonalToken(surveyRef, me.userId(), me.email());
    String inviteLink = buildInviteLink(req, id, plain);
    return new MyTokenDto(true, inviteLink);
  }

  @PostMapping("/{id}/download-tokens")
  public ResponseEntity<Map<String, String>> createDownloadLink(
      @AuthenticationPrincipal AuthUser me,
      @PathVariable UUID id,
      HttpServletRequest req) {
    if (me == null || me.userId() == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    boolean isAdmin = hasRole("ROLE_ADMIN");
    boolean isLeaderOfTeam = surveyRepo.existsByIdAndTeam_Members_User_IdAndTeam_Members_LeaderTrue(id, me.userId());
    if (!(isAdmin || isLeaderOfTeam))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);

    String dl = downloadTokens.issue(id, me.userId(), java.time.Duration.ofMinutes(5));
    String base = (frontendBaseUrl != null && !frontendBaseUrl.isBlank())
        ? frontendBaseUrl.replaceAll("/+$", "")
        : buildBaseFromRequest(req);
    String url = base + "/api/surveys/" + id + "/results/download?dl=" + dl;

    return ResponseEntity
        .created(URI.create(url))
        .body(Map.of("url", url));
  }

  @GetMapping("/{id}/results/download")
  public ResponseEntity<byte[]> downloadViaToken(@PathVariable UUID id, @RequestParam("dl") String dl)
      throws Exception {
    UUID userId = downloadTokens.verifyAndExtractUser(dl, id);
    boolean allowed = hasRole("ROLE_ADMIN") ||
        surveyRepo.existsByIdAndTeam_Members_User_IdAndTeam_Members_LeaderTrue(id, userId);
    if (!allowed)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);

    SurveyResultsDto dto = surveyService.getResults(id);
    byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(dto);
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"survey-" + id + "-results.json\"")
        .header("Cache-Control", "no-store")
        .header("Pragma", "no-cache")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(json);
  }

  private String buildBaseFromRequest(HttpServletRequest req) {
    String scheme = req.getScheme();
    String host = req.getServerName();
    int port = req.getServerPort();
    boolean isDefault = ("http".equalsIgnoreCase(scheme) && port == 80) ||
        ("https".equalsIgnoreCase(scheme) && port == 443);
    return scheme + "://" + host + (isDefault ? "" : (":" + port));
  }

  // WICHTIG: Invite-Link soll die SPA öffnen (ohne /api)
  private String buildInviteLink(HttpServletRequest req, UUID surveyId, String plain) {
    String base = (frontendBaseUrl != null && !frontendBaseUrl.isBlank())
        ? frontendBaseUrl.replaceAll("/+$", "")
        : buildBaseFromRequest(req);
    return base + "/surveys/" + surveyId + "?token=" + URLEncoder.encode(plain, StandardCharsets.UTF_8);
  }
}

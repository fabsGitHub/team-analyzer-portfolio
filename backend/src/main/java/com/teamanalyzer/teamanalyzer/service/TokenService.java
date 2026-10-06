package com.teamanalyzer.teamanalyzer.service;

import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.teamanalyzer.teamanalyzer.domain.Survey;
import com.teamanalyzer.teamanalyzer.domain.SurveyResponse;
import com.teamanalyzer.teamanalyzer.domain.SurveyToken;
import com.teamanalyzer.teamanalyzer.domain.User;
import com.teamanalyzer.teamanalyzer.port.AppClock;
import com.teamanalyzer.teamanalyzer.port.DigestService;
import com.teamanalyzer.teamanalyzer.repo.SurveyRepository;
import com.teamanalyzer.teamanalyzer.repo.SurveyTokenRepository;
import com.teamanalyzer.teamanalyzer.repo.TeamMemberRepository;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TokenService.class);

    private final SurveyTokenRepository tokenRepo;
    private final SurveyRepository surveyRepo;
    private final TeamMemberRepository tmRepo;
    private final AppClock clock;

    @PersistenceContext
    private EntityManager em;
    private final DigestService digest; // <— Port nutzen

    @Transactional
    public SurveyToken acquireForSubmission(UUID surveyId, String tokenHashHex) {
        final byte[] hash;
        try {
            hash = HexFormat.of().parseHex(tokenHashHex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
        }

        SurveyToken tok = tokenRepo
                .findWithLockByTokenHashAndSurvey_Id(hash, surveyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));

        if (tok.isRevoked() || tok.isRedeemed()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token already used");
        }
        return tok; // locked until tx ends
    }

    @Transactional
    public void consume(SurveyToken tok) {
        tok.setRedeemed(true);
        tok.setRedeemedAt(clock.now());
        tokenRepo.save(tok);
    }

    @Transactional
    public String ensurePersonalToken(Survey survey, UUID userId, @Nullable String email) {
        var existing = tokenRepo.findFirstBySurvey_IdAndIssuedToUser_IdAndRedeemedFalseAndRevokedFalse(
                survey.getId(), userId);
        if (existing.isPresent())
            return null;

        String plain = UUID.randomUUID().toString();
        byte[] hash = digest.sha256(plain);
        log.info("ISSUE new token: hash={}", HexFormat.of().formatHex(hash)); // kein plain!

        var tok = new SurveyToken();
        tok.setSurvey(survey);
        tok.setTokenHash(hash);
        tok.setIssuedAt(clock.now());
        tok.setIssuedToEmail(email);
        tok.setIssuedToUser(em.getReference(User.class, userId));
        tokenRepo.save(tok);
        return plain;
    }

    @Transactional
    public int revokeOpenTokensForUser(UUID surveyId, UUID userId) {
        // Name im Repo: revokeAllActiveForUser (s. refactor unten)
        return tokenRepo.revokeAllActiveForUser(surveyId, userId);
    }

    @Transactional
    public String renewPersonalToken(Survey survey, UUID userId, @Nullable String email) {
        revokeOpenTokensForUser(survey.getId(), userId);
        var plain = ensurePersonalToken(survey, userId, email);
        if (plain == null) {
            revokeOpenTokensForUser(survey.getId(), userId);
            plain = ensurePersonalToken(survey, userId, email);
        }
        if (plain == null)
            throw new IllegalStateException("Could not renew token deterministically");
        return plain;
    }

    @Transactional(readOnly = true)
    public List<SurveyToken> listOpenTokensForUser(UUID userId) {
        return tokenRepo.findOpenByUser(userId);
    }

    @Transactional(readOnly = true)
    public Optional<SurveyToken> findActivePersonalToken(UUID surveyId, UUID userId) {
        return tokenRepo.findFirstBySurvey_IdAndIssuedToUser_IdAndRedeemedFalseAndRevokedFalse(surveyId, userId);
    }

    @Transactional
    public int ensureTokensForAllTeamMembers(UUID surveyId) {
        var teamId = surveyRepo.findTeamIdById(surveyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var members = tmRepo.findByTeam_Id(teamId);
        var surveyRef = Survey.ref(surveyId);
        int created = 0;
        for (var m : members) {
            var uid = m.getUser().getId();
            var email = m.getUser().getEmail();
            var plain = ensurePersonalToken(surveyRef, uid, email);
            if (plain != null)
                created++;
        }
        return created;
    }

    @Transactional
    public SurveyToken redeem(UUID surveyId, String plainToken) {
        // nur Hash loggen
        byte[] hash = digest.sha256(plainToken);
        log.info("REDEEM token: surveyId={} hash={}", surveyId, HexFormat.of().formatHex(hash));

        var tok = tokenRepo.findByTokenHashAndSurvey_Id(hash, surveyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));
        if (tok.isRevoked() || tok.isRedeemed())
            throw new ResponseStatusException(HttpStatus.GONE, "Token already used");

        tok.setRedeemed(true);
        tok.setRedeemedAt(clock.now());
        return tokenRepo.save(tok);
    }

    @Transactional
    public void consumeAndDeleteToken(SurveyResponse response, SurveyToken token) {
        response.clearToken(); // statt setToken(null)
        tokenRepo.delete(token);
    }
}

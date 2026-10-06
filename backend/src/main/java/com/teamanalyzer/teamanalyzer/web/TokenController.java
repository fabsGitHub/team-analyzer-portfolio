package com.teamanalyzer.teamanalyzer.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.teamanalyzer.teamanalyzer.security.AuthUser;
import com.teamanalyzer.teamanalyzer.service.TokenService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    public record MyOpenTokenDto(UUID tokenId, UUID surveyId, String surveyTitle, Instant issuedAt) {
    }

    @GetMapping("/tokens")
    public List<MyOpenTokenDto> myOpenTokens(@AuthenticationPrincipal AuthUser me) {
        if (me == null || me.userId() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        return tokenService.listOpenTokensForUser(me.userId()).stream()
                .map(t -> new MyOpenTokenDto(
                        t.getId(),
                        t.getSurvey().getId(),
                        t.getSurvey().getTitle(),
                        t.getIssuedAt()))
                .toList();
    }
}

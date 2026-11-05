package com.teamanalyzer.teamanalyzer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamanalyzer.teamanalyzer.MutableTestClock;
import com.teamanalyzer.teamanalyzer.infra.crypto.HmacTokenSigner;
import com.teamanalyzer.teamanalyzer.support.TestSigningSecrets;

class DownloadTokenServiceTest {

    private final MutableTestClock clock = new MutableTestClock(Instant.parse("2026-01-01T10:00:00Z"));
    private final HmacTokenSigner signer = new HmacTokenSigner(TestSigningSecrets.randomBase64(48));
    private final DownloadTokenService service = new DownloadTokenService(new ObjectMapper(), signer, clock);

    private final UUID survey = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();

    @Test
    void issued_token_round_trips_to_the_user() {
        String token = service.issue(survey, user, Duration.ofMinutes(5));
        assertThat(service.verifyAndExtractUser(token, survey)).isEqualTo(user);
    }

    @Test
    void rejects_tampered_wrong_survey_and_expired_tokens() {
        String token = service.issue(survey, user, Duration.ofMinutes(5));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> service.verifyAndExtractUser(tampered, survey))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.verifyAndExtractUser(token, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);

        clock.plus(Duration.ofMinutes(6));
        assertThatThrownBy(() -> service.verifyAndExtractUser(token, survey))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void signer_rejects_short_secrets() {
        assertThatThrownBy(() -> new HmacTokenSigner("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void signing_is_safe_under_concurrency() throws Exception {
        String payload = "eyJzaWQiOiJ4In0";
        String expected = signer.signUrlSafe(payload);

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> jobs = new ArrayList<>();
            for (int i = 0; i < 2_000; i++) {
                jobs.add(() -> signer.matches(payload, expected));
            }
            for (Future<Boolean> f : pool.invokeAll(jobs)) {
                assertThat(f.get()).isTrue();
            }
        } finally {
            pool.shutdownNow();
        }
    }
}

package com.teamanalyzer.teamanalyzer.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class DemoStartLimiterTest {

    private static final Duration HOUR = Duration.ofHours(1);
    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void allows_the_limit_per_address_in_a_sliding_window() {
        DemoStartLimiter limiter = new DemoStartLimiter(2, HOUR, 100);

        assertThat(limiter.tryAcquire("a", T0)).isEmpty();
        assertThat(limiter.tryAcquire("a", T0.plusSeconds(600))).isEmpty();
        // Full until the first start leaves the window.
        assertThat(limiter.tryAcquire("a", T0.plusSeconds(900))).contains(Duration.ofMinutes(45));
        assertThat(limiter.tryAcquire("b", T0.plusSeconds(900))).isEmpty();

        assertThat(limiter.tryAcquire("a", T0.plus(HOUR).plusSeconds(1))).isEmpty();
        assertThat(limiter.tryAcquire("a", T0.plus(HOUR).plusSeconds(2))).isPresent();
    }

    @Test
    void a_flood_of_addresses_neither_grows_memory_nor_locks_out_new_ones() {
        DemoStartLimiter limiter = new DemoStartLimiter(2, HOUR, 10);

        for (int i = 0; i < 10_000; i++) {
            assertThat(limiter.tryAcquire("flood-" + i, T0.plusMillis(i))).isEmpty();
        }

        assertThat(limiter.trackedAddresses()).isEqualTo(10);
        assertThat(limiter.tryAcquire("visitor", T0.plusSeconds(60))).isEmpty();
        assertThat(limiter.tryAcquire("visitor", T0.plusSeconds(61))).isEmpty();
        assertThat(limiter.tryAcquire("visitor", T0.plusSeconds(62))).isPresent();
    }

    @Test
    void the_most_recently_seen_addresses_are_kept() {
        DemoStartLimiter limiter = new DemoStartLimiter(1, HOUR, 2);

        limiter.tryAcquire("busy", T0);
        limiter.tryAcquire("other", T0.plusSeconds(1));
        // A rejected attempt counts as recent activity, so "busy" stays tracked ...
        assertThat(limiter.tryAcquire("busy", T0.plusSeconds(2))).isPresent();
        limiter.tryAcquire("new", T0.plusSeconds(3));
        // ... and "other", seen least recently, was forgotten instead.
        assertThat(limiter.tryAcquire("busy", T0.plusSeconds(4))).isPresent();
        assertThat(limiter.tryAcquire("other", T0.plusSeconds(5))).isEmpty();
    }

    @Test
    void addresses_without_a_start_in_the_window_are_forgotten() {
        DemoStartLimiter limiter = new DemoStartLimiter(2, HOUR, 100);
        limiter.tryAcquire("a", T0);
        limiter.tryAcquire("b", T0.plusSeconds(1));

        limiter.tryAcquire("c", T0.plus(HOUR).plusSeconds(1));

        assertThat(limiter.trackedAddresses()).isEqualTo(1);
    }
}

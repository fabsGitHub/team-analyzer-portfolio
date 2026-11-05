package com.teamanalyzer.teamanalyzer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserEmailNormalizationTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "J̌@example.test",      // J + caron: no precomposed capital, lower case has one
            "W̊@example.test",      // W + ring above
            "İ̖@example.test", // dotted capital I + combining mark below
            "José@Example.test",   // NFD
            "  Mixed.Case@Example.TEST ",
            "plain@example.test" })
    void normalisation_is_idempotent(String raw) {
        String once = User.normalizeEmail(raw);
        assertThat(User.normalizeEmail(once)).isEqualTo(once);
    }

    @ParameterizedTest
    @ValueSource(strings = { "José@example.test", "josé@EXAMPLE.test" })
    void composed_and_decomposed_spellings_are_the_same_address(String raw) {
        assertThat(User.normalizeEmail(raw)).isEqualTo("josé@example.test");
    }
}

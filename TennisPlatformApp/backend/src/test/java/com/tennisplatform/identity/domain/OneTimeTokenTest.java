package com.tennisplatform.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OneTimeTokenTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant EXPIRY = NOW.plusSeconds(3600);
    private static final UUID USER = UUID.randomUUID();

    @Test
    void isUsableBeforeExpiryAndNotAfter() {
        OneTimeToken token = OneTimeToken.issue(USER, "hash", EXPIRY);

        assertThat(token.isUsable(NOW)).isTrue();
        assertThat(token.isUsable(EXPIRY)).isFalse();
    }

    @Test
    void consumingMarksItUsed() {
        OneTimeToken token = OneTimeToken.issue(USER, "hash", EXPIRY);
        token.consume(NOW);

        assertThat(token.usedAt()).isEqualTo(NOW);
        assertThat(token.isUsable(NOW)).isFalse();
    }

    /** Defence in depth: consuming refuses even if a caller forgot to check first. */
    @Test
    void cannotBeConsumedTwice() {
        OneTimeToken token = OneTimeToken.issue(USER, "hash", EXPIRY);
        token.consume(NOW);

        assertThatThrownBy(() -> token.consume(NOW)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void cannotBeConsumedAfterExpiry() {
        OneTimeToken token = OneTimeToken.issue(USER, "hash", EXPIRY);

        assertThatThrownBy(() -> token.consume(EXPIRY)).isInstanceOf(InvalidTokenException.class);
    }
}

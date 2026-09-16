package com.tennisplatform.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(3600);
    private static final UUID USER = UUID.randomUUID();

    @Test
    void aFreshTokenIsActive() {
        RefreshToken token = RefreshToken.startFamily(USER, "hash", NOW, LATER);

        assertThat(token.isActive(NOW)).isTrue();
        assertThat(token.wasAlreadyRotated()).isFalse();
    }

    @Test
    void anExpiredTokenIsNotActive() {
        RefreshToken token = RefreshToken.startFamily(USER, "hash", NOW, LATER);

        assertThat(token.isActive(LATER)).isFalse();
        assertThat(token.isActive(LATER.plusSeconds(1))).isFalse();
    }

    @Test
    void aRevokedTokenIsNotActive() {
        RefreshToken token = RefreshToken.startFamily(USER, "hash", NOW, LATER);
        token.revoke(NOW);

        assertThat(token.isActive(NOW)).isFalse();
    }

    /** Rotation is what turns a later presentation of the old token into a theft signal. */
    @Test
    void rotatingMarksThePredecessorAsUsedAndRevoked() {
        RefreshToken first = RefreshToken.startFamily(USER, "hash-1", NOW, LATER);
        RefreshToken second = RefreshToken.continueFamily(USER, "hash-2", first.familyId(), NOW, LATER);

        first.replaceWith(second, NOW);

        assertThat(first.wasAlreadyRotated()).isTrue();
        assertThat(first.isActive(NOW)).isFalse();
        assertThat(first.replacedByTokenId()).isEqualTo(second.id());
    }

    @Test
    void successorsStayInTheSameFamilySoTheChainCanBeRevokedAtOnce() {
        RefreshToken first = RefreshToken.startFamily(USER, "hash-1", NOW, LATER);
        RefreshToken second = RefreshToken.continueFamily(USER, "hash-2", first.familyId(), NOW, LATER);

        assertThat(second.familyId()).isEqualTo(first.familyId());
    }

    @Test
    void eachLoginStartsAnIndependentFamily() {
        RefreshToken one = RefreshToken.startFamily(USER, "hash-1", NOW, LATER);
        RefreshToken other = RefreshToken.startFamily(USER, "hash-2", NOW, LATER);

        assertThat(one.familyId()).isNotEqualTo(other.familyId());
    }

    @Test
    void revokingTwiceKeepsTheFirstTimestamp() {
        RefreshToken token = RefreshToken.startFamily(USER, "hash", NOW, LATER);
        token.revoke(NOW);
        token.revoke(NOW.plusSeconds(60));

        assertThat(token.revokedAt()).isEqualTo(NOW);
    }
}

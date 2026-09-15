package com.tennisplatform.identity.application.port.out;

import com.tennisplatform.identity.domain.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokens {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revokes every token in a rotation chain. Used when reuse is detected. */
    void revokeFamily(UUID familyId, Instant now);

    /** Revokes every session of a user. Used on password reset and on account disable. */
    void revokeAllForUser(UUID userId, Instant now);
}

package com.tennisplatform.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Shared shape of the email verification and password reset tokens: single use, expiring,
 * and stored only as a hash. The raw value exists just once, in the email that was sent.
 */
public class OneTimeToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant usedAt;

    private OneTimeToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant usedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    public static OneTimeToken issue(UUID userId, String tokenHash, Instant expiresAt) {
        return new OneTimeToken(UUID.randomUUID(), userId, tokenHash, expiresAt, null);
    }

    public static OneTimeToken rehydrate(UUID id, UUID userId, String tokenHash,
                                         Instant expiresAt, Instant usedAt) {
        return new OneTimeToken(id, userId, tokenHash, expiresAt, usedAt);
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    /**
     * Callers must check {@link #isUsable} first; this refuses to consume a spent token so a
     * missing check cannot silently allow reuse.
     */
    public void consume(Instant now) {
        if (usedAt != null) {
            throw new InvalidTokenException("Token has already been used");
        }
        if (!now.isBefore(expiresAt)) {
            throw new InvalidTokenException("Token has expired");
        }
        this.usedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant usedAt() {
        return usedAt;
    }
}

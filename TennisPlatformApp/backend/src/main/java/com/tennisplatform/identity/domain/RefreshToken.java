package com.tennisplatform.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One link in a rotation chain. Every login starts a new family; every refresh replaces the
 * current token with a successor in the same family.
 *
 * <p>Presenting a token that already has a successor means two parties hold the same token:
 * the legitimate user and someone who stole it. There is no way to tell which one is asking,
 * so the whole family is revoked and both must sign in again. Forcing the victim to
 * re-authenticate is the acceptable cost of locking out the attacker.
 */
public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final UUID familyId;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant revokedAt;
    private UUID replacedByTokenId;

    private RefreshToken(UUID id, UUID userId, String tokenHash, UUID familyId, Instant issuedAt,
                         Instant expiresAt, Instant revokedAt, UUID replacedByTokenId) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
    }

    /** Starts a brand new rotation chain; used at login. */
    public static RefreshToken startFamily(UUID userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, UUID.randomUUID(),
                issuedAt, expiresAt, null, null);
    }

    /** Continues an existing chain; used at refresh. */
    public static RefreshToken continueFamily(UUID userId, String tokenHash, UUID familyId,
                                              Instant issuedAt, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, familyId,
                issuedAt, expiresAt, null, null);
    }

    public static RefreshToken rehydrate(UUID id, UUID userId, String tokenHash, UUID familyId,
                                         Instant issuedAt, Instant expiresAt, Instant revokedAt,
                                         UUID replacedByTokenId) {
        return new RefreshToken(id, userId, tokenHash, familyId, issuedAt, expiresAt,
                revokedAt, replacedByTokenId);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && replacedByTokenId == null && now.isBefore(expiresAt);
    }

    /** True when this token was already exchanged for a successor - the theft signal. */
    public boolean wasAlreadyRotated() {
        return replacedByTokenId != null;
    }

    public void replaceWith(RefreshToken successor, Instant now) {
        this.replacedByTokenId = successor.id();
        this.revokedAt = now;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            this.revokedAt = now;
        }
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

    public UUID familyId() {
        return familyId;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public UUID replacedByTokenId() {
        return replacedByTokenId;
    }
}

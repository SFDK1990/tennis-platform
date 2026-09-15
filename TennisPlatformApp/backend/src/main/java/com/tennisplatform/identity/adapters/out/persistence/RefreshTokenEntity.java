package com.tennisplatform.identity.adapters.out.persistence;

import com.tennisplatform.identity.domain.RefreshToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    protected RefreshTokenEntity() {
    }

    static RefreshTokenEntity fromDomain(RefreshToken token) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.id = token.id();
        entity.userId = token.userId();
        entity.tokenHash = token.tokenHash();
        entity.familyId = token.familyId();
        entity.issuedAt = token.issuedAt();
        entity.expiresAt = token.expiresAt();
        entity.revokedAt = token.revokedAt();
        entity.replacedByTokenId = token.replacedByTokenId();
        return entity;
    }

    RefreshToken toDomain() {
        return RefreshToken.rehydrate(id, userId, tokenHash, familyId, issuedAt, expiresAt,
                revokedAt, replacedByTokenId);
    }
}

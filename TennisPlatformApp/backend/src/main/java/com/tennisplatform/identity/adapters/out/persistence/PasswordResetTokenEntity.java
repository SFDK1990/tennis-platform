package com.tennisplatform.identity.adapters.out.persistence;

import com.tennisplatform.identity.domain.OneTimeToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordResetTokenEntity() {
    }

    static PasswordResetTokenEntity fromDomain(OneTimeToken token) {
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.id = token.id();
        entity.userId = token.userId();
        entity.tokenHash = token.tokenHash();
        entity.expiresAt = token.expiresAt();
        entity.usedAt = token.usedAt();
        entity.createdAt = Instant.now();
        return entity;
    }

    OneTimeToken toDomain() {
        return OneTimeToken.rehydrate(id, userId, tokenHash, expiresAt, usedAt);
    }
}

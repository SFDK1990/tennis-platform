package com.tennisplatform.identity.adapters.out.persistence;

import com.tennisplatform.identity.domain.OneTimeToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verifications")
class EmailVerificationEntity {

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

    protected EmailVerificationEntity() {
    }

    static EmailVerificationEntity fromDomain(OneTimeToken token) {
        EmailVerificationEntity entity = new EmailVerificationEntity();
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

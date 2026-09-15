package com.tennisplatform.identity.adapters.out.persistence;

import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.domain.RefreshToken;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class RefreshTokensAdapter implements RefreshTokens {

    private final RefreshTokenJpaRepository jpa;

    RefreshTokensAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return jpa.save(RefreshTokenEntity.fromDomain(token)).toDomain();
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenEntity::toDomain);
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
        jpa.revokeFamily(familyId, now);
    }

    @Override
    public void revokeAllForUser(UUID userId, Instant now) {
        jpa.revokeAllForUser(userId, now);
    }
}

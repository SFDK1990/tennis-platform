package com.tennisplatform.identity.adapters.out.persistence;

import com.tennisplatform.identity.application.port.out.PasswordResetTokens;
import com.tennisplatform.identity.domain.OneTimeToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class PasswordResetTokensAdapter implements PasswordResetTokens {

    private final PasswordResetTokenJpaRepository jpa;

    PasswordResetTokensAdapter(PasswordResetTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OneTimeToken save(OneTimeToken token) {
        return jpa.save(PasswordResetTokenEntity.fromDomain(token)).toDomain();
    }

    @Override
    public Optional<OneTimeToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(PasswordResetTokenEntity::toDomain);
    }
}

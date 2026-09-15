package com.tennisplatform.identity.adapters.out.persistence;

import com.tennisplatform.identity.application.port.out.EmailVerificationTokens;
import com.tennisplatform.identity.domain.OneTimeToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class EmailVerificationTokensAdapter implements EmailVerificationTokens {

    private final EmailVerificationJpaRepository jpa;

    EmailVerificationTokensAdapter(EmailVerificationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OneTimeToken save(OneTimeToken token) {
        return jpa.save(EmailVerificationEntity.fromDomain(token)).toDomain();
    }

    @Override
    public Optional<OneTimeToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(EmailVerificationEntity::toDomain);
    }
}

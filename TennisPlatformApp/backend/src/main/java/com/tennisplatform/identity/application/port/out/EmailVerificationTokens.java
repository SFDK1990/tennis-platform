package com.tennisplatform.identity.application.port.out;

import com.tennisplatform.identity.domain.OneTimeToken;

import java.util.Optional;

public interface EmailVerificationTokens {

    OneTimeToken save(OneTimeToken token);

    Optional<OneTimeToken> findByTokenHash(String tokenHash);
}

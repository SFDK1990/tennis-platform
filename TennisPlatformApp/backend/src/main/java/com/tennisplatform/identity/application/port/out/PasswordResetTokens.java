package com.tennisplatform.identity.application.port.out;

import com.tennisplatform.identity.domain.OneTimeToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokens {

    OneTimeToken save(OneTimeToken token);

    Optional<OneTimeToken> findByTokenHash(String tokenHash);

    /** Marks every unused link of the user as used, so none of them works any more. */
    void spendAllForUser(UUID userId, Instant now);
}

package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.Logout;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class LogoutService implements Logout {

    private final RefreshTokens refreshTokens;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    public LogoutService(RefreshTokens refreshTokens, TokenHasher tokenHasher, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
    }

    /**
     * Revokes the whole family, not just the presented token: logging out means ending that
     * session, and any successor already issued in the same chain must die with it.
     *
     * <p>Silent on unknown tokens - logging out is not a place to confirm whether a token was
     * real, and the caller's intent is satisfied either way.
     */
    @Override
    @Transactional
    public void logout(Command command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(tokenHasher.hash(command.refreshToken()))
                .ifPresent(token -> refreshTokens.revokeFamily(token.familyId(), clock.instant()));
    }
}

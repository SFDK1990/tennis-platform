package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.AuthenticationResult;
import com.tennisplatform.identity.application.port.in.RefreshSession;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.identity.application.port.out.AccessTokenIssuer;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.InvalidTokenException;
import com.tennisplatform.identity.domain.RefreshToken;
import com.tennisplatform.identity.domain.TokenReuseDetectedException;
import com.tennisplatform.identity.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class RefreshSessionService implements RefreshSession {

    private static final Logger log = LoggerFactory.getLogger(RefreshSessionService.class);

    private final RefreshTokens refreshTokens;
    private final UserRepository users;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public RefreshSessionService(RefreshTokens refreshTokens, UserRepository users,
                                 SecureTokenGenerator tokenGenerator, TokenHasher tokenHasher,
                                 AccessTokenIssuer accessTokenIssuer, Clock clock,
                                 Duration refreshTokenTtl) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * {@code noRollbackFor} is load bearing, not a detail. Both failure paths below revoke a
     * token family and then throw; without this, the rollback triggered by that exception
     * would undo the revocation, and detecting a stolen token would have no effect whatsoever.
     */
    @Override
    @Transactional(noRollbackFor = {TokenReuseDetectedException.class, InvalidTokenException.class})
    public AuthenticationResult refresh(Command command) {
        Instant now = clock.instant();

        RefreshToken current = refreshTokens.findByTokenHash(tokenHasher.hash(command.refreshToken()))
                .orElseThrow(() -> new InvalidTokenException("Unknown refresh token"));

        if (current.wasAlreadyRotated()) {
            // Two parties hold the same token and there is no way to tell which is legitimate.
            // The family id is safe to log; the token itself never is.
            log.warn("Refresh token reuse detected, revoking family {}", current.familyId());
            refreshTokens.revokeFamily(current.familyId(), now);
            throw new TokenReuseDetectedException(current.familyId());
        }

        if (!current.isActive(now)) {
            throw new InvalidTokenException("Refresh token is revoked or expired");
        }

        User user = users.findById(current.userId())
                .orElseThrow(() -> new InvalidTokenException("Unknown refresh token"));

        if (!user.canAuthenticate()) {
            // Disabled mid-session: kill the whole chain instead of letting it roll on.
            refreshTokens.revokeFamily(current.familyId(), now);
            throw new InvalidTokenException("Account is not active");
        }

        return rotate(current, user, now);
    }

    private AuthenticationResult rotate(RefreshToken current, User user, Instant now) {
        Instant expiresAt = now.plus(refreshTokenTtl);
        String rawToken = tokenGenerator.generate();

        RefreshToken successor = refreshTokens.save(RefreshToken.continueFamily(
                user.id(), tokenHasher.hash(rawToken), current.familyId(), now, expiresAt));

        current.replaceWith(successor, now);
        refreshTokens.save(current);

        return new AuthenticationResult(
                accessTokenIssuer.issue(user),
                now.plus(accessTokenIssuer.lifetime()),
                rawToken,
                expiresAt,
                UserSummary.of(user));
    }
}

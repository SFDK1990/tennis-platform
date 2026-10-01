package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.AuthenticationResult;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.identity.application.port.out.AccessTokenIssuer;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.domain.RefreshToken;
import com.tennisplatform.identity.domain.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Opens a new session: an access token and the first refresh token of a new family. Logging in
 * and changing the password both end with one, and they must not drift apart.
 */
class SessionIssuer {

    private final RefreshTokens refreshTokens;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    SessionIssuer(RefreshTokens refreshTokens, SecureTokenGenerator tokenGenerator, TokenHasher tokenHasher,
                  AccessTokenIssuer accessTokenIssuer, Clock clock, Duration refreshTokenTtl) {
        this.refreshTokens = refreshTokens;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    AuthenticationResult issueFor(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(refreshTokenTtl);
        String rawRefreshToken = tokenGenerator.generate();

        refreshTokens.save(RefreshToken.startFamily(
                user.id(), tokenHasher.hash(rawRefreshToken), now, expiresAt));

        return new AuthenticationResult(
                accessTokenIssuer.issue(user),
                now.plus(accessTokenIssuer.lifetime()),
                rawRefreshToken,
                expiresAt,
                UserSummary.of(user));
    }
}

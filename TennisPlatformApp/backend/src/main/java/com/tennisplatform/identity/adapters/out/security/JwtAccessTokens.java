package com.tennisplatform.identity.adapters.out.security;

import com.tennisplatform.identity.application.port.out.AccessTokenIssuer;
import com.tennisplatform.identity.configuration.IdentityProperties;
import com.tennisplatform.identity.domain.Role;
import com.tennisplatform.identity.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies access tokens. Symmetric signing (HS256) is deliberate: the same
 * service both issues and verifies, so an asymmetric key pair would add key management
 * without adding a guarantee. Switching to RS256 becomes worthwhile only once a third party
 * needs to verify tokens it cannot mint.
 */
@Component
public class JwtAccessTokens implements AccessTokenIssuer {

    static final String ROLE_CLAIM = "role";
    static final String EMAIL_VERIFIED_CLAIM = "email_verified";

    private final SecretKey key;
    private final String issuer;
    private final Duration lifetime;
    private final Clock clock;

    JwtAccessTokens(IdentityProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.getJwtIssuer();
        this.lifetime = properties.getAccessTokenTtl();
        this.clock = clock;
    }

    @Override
    public String issue(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.id().toString())
                .issuer(issuer)
                .claim(ROLE_CLAIM, user.role().name())
                .claim(EMAIL_VERIFIED_CLAIM, user.isEmailVerified())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(key)
                .compact();
    }

    @Override
    public Duration lifetime() {
        return lifetime;
    }

    /**
     * Returns empty for anything not provably ours and current: bad signature, wrong issuer,
     * expired, or malformed. Callers treat all of those identically.
     */
    public Optional<AuthenticatedUser> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    Role.valueOf(claims.get(ROLE_CLAIM, String.class)),
                    Boolean.TRUE.equals(claims.get(EMAIL_VERIFIED_CLAIM, Boolean.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

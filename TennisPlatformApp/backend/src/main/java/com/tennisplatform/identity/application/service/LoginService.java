package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.AuthenticationResult;
import com.tennisplatform.identity.application.port.in.Login;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.identity.application.port.out.AccessTokenIssuer;
import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.InvalidCredentialsException;
import com.tennisplatform.identity.domain.RefreshToken;
import com.tennisplatform.identity.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class LoginService implements Login {

    private final UserRepository users;
    private final RefreshTokens refreshTokens;
    private final PasswordHasher passwordHasher;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public LoginService(UserRepository users, RefreshTokens refreshTokens, PasswordHasher passwordHasher,
                        SecureTokenGenerator tokenGenerator, TokenHasher tokenHasher,
                        AccessTokenIssuer accessTokenIssuer, Clock clock, Duration refreshTokenTtl) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Override
    @Transactional
    public AuthenticationResult login(Command command) {
        Optional<User> found = parseEmail(command.email()).flatMap(users::findByEmail);

        if (found.isEmpty()) {
            // Hash anyway so an unknown email costs the same time as a known one.
            passwordHasher.burnTime(command.password());
            throw new InvalidCredentialsException();
        }

        User user = found.get();
        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        // A disabled account reports the same failure as a wrong password: saying "disabled"
        // would confirm the address is registered.
        if (!user.canAuthenticate()) {
            throw new InvalidCredentialsException();
        }

        return issueSession(user);
    }

    private AuthenticationResult issueSession(User user) {
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

    /** A malformed address is a failed login, not a validation error: same response either way. */
    private Optional<EmailAddress> parseEmail(String raw) {
        try {
            return Optional.of(new EmailAddress(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

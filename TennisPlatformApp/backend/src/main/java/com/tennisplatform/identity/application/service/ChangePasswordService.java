package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.AuthenticationResult;
import com.tennisplatform.identity.application.port.in.ChangePassword;
import com.tennisplatform.identity.application.port.out.AccessTokenIssuer;
import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.CurrentPasswordIncorrectException;
import com.tennisplatform.identity.domain.PasswordPolicy;
import com.tennisplatform.identity.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

public class ChangePasswordService implements ChangePassword {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordService.class);

    private final UserRepository users;
    private final RefreshTokens refreshTokens;
    private final PasswordHasher passwordHasher;
    private final SessionIssuer sessions;
    private final Clock clock;

    public ChangePasswordService(UserRepository users, RefreshTokens refreshTokens, PasswordHasher passwordHasher,
                                 SecureTokenGenerator tokenGenerator, TokenHasher tokenHasher,
                                 AccessTokenIssuer accessTokenIssuer, Clock clock, Duration refreshTokenTtl) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.sessions = new SessionIssuer(refreshTokens, tokenGenerator, tokenHasher, accessTokenIssuer,
                clock, refreshTokenTtl);
        this.clock = clock;
    }

    /**
     * Everything that can refuse runs before anything is written, so a refusal leaves the
     * sessions as they were: revoking and then throwing inside one transaction would undo the
     * revocation anyway.
     */
    @Override
    @Transactional
    public AuthenticationResult change(Command command) {
        User user = users.findById(command.userId())
                .orElseThrow(CurrentPasswordIncorrectException::new);
        if (!passwordHasher.matches(command.currentPassword(), user.passwordHash())) {
            throw new CurrentPasswordIncorrectException();
        }
        PasswordPolicy.validate(command.newPassword());

        user.changePassword(passwordHasher.hash(command.newPassword()));
        users.save(user);
        refreshTokens.revokeAllForUser(user.id(), clock.instant());
        log.info("Password of account {} changed, its other sessions ended", user.id());
        return sessions.issueFor(user);
    }
}

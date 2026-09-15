package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.ResetPassword;
import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.PasswordResetTokens;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.InvalidTokenException;
import com.tennisplatform.identity.domain.OneTimeToken;
import com.tennisplatform.identity.domain.PasswordPolicy;
import com.tennisplatform.identity.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class ResetPasswordService implements ResetPassword {

    private final PasswordResetTokens resetTokens;
    private final UserRepository users;
    private final RefreshTokens refreshTokens;
    private final PasswordHasher passwordHasher;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    public ResetPasswordService(PasswordResetTokens resetTokens, UserRepository users,
                                RefreshTokens refreshTokens, PasswordHasher passwordHasher,
                                TokenHasher tokenHasher, Clock clock) {
        this.resetTokens = resetTokens;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void reset(Command command) {
        PasswordPolicy.validate(command.newPassword());
        Instant now = clock.instant();

        OneTimeToken token = resetTokens.findByTokenHash(tokenHasher.hash(command.token()))
                .orElseThrow(() -> new InvalidTokenException("Unknown reset token"));

        token.consume(now);
        resetTokens.save(token);

        User user = users.findById(token.userId())
                .orElseThrow(() -> new InvalidTokenException("Unknown reset token"));

        user.changePassword(passwordHasher.hash(command.newPassword()));
        users.save(user);

        // A reset usually means the account may be compromised. Leaving existing sessions alive
        // would defeat the purpose, so every one of them is revoked.
        refreshTokens.revokeAllForUser(user.id(), now);
    }
}

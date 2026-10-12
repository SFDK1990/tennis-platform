package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.CloseAccount;
import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.PasswordResetTokens;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.CurrentPasswordIncorrectException;
import com.tennisplatform.identity.domain.Role;
import com.tennisplatform.identity.domain.User;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class CloseAccountService implements CloseAccount {

    private final UserRepository users;
    private final RefreshTokens refreshTokens;
    private final PasswordResetTokens resetTokens;
    private final PasswordHasher passwordHasher;
    private final SecureTokenGenerator tokenGenerator;
    private final Clock clock;

    public CloseAccountService(UserRepository users, RefreshTokens refreshTokens, PasswordResetTokens resetTokens,
                               PasswordHasher passwordHasher, SecureTokenGenerator tokenGenerator, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
    }

    /**
     * Only a student's account closes: the teacher and the administrator are created by the
     * bootstrap, and the platform does not run without them. The new hash is of a random value
     * nobody ever saw, so no password can match it.
     */
    @Override
    @Transactional
    public void close(UUID userId, String password) {
        User user = users.findById(userId).orElseThrow(CurrentPasswordIncorrectException::new);
        if (user.role() != Role.STUDENT) {
            throw ForbiddenOperationException.roleNotAllowed("Only a student can delete their own account");
        }
        if (!passwordHasher.matches(password, user.passwordHash())) {
            throw new CurrentPasswordIncorrectException();
        }

        Instant now = clock.instant();
        user.close(passwordHasher.hash(tokenGenerator.generate()));
        users.save(user);
        refreshTokens.revokeAllForUser(userId, now);
        resetTokens.spendAllForUser(userId, now);
    }
}

package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.VerifyEmail;
import com.tennisplatform.identity.application.port.out.EmailVerificationTokens;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.InvalidTokenException;
import com.tennisplatform.identity.domain.OneTimeToken;
import com.tennisplatform.identity.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class VerifyEmailService implements VerifyEmail {

    private final EmailVerificationTokens verificationTokens;
    private final UserRepository users;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    public VerifyEmailService(EmailVerificationTokens verificationTokens, UserRepository users,
                              TokenHasher tokenHasher, Clock clock) {
        this.verificationTokens = verificationTokens;
        this.users = users;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void verify(Command command) {
        Instant now = clock.instant();

        OneTimeToken token = verificationTokens.findByTokenHash(tokenHasher.hash(command.token()))
                .orElseThrow(() -> new InvalidTokenException("Unknown verification token"));

        token.consume(now);
        verificationTokens.save(token);

        User user = users.findById(token.userId())
                .orElseThrow(() -> new InvalidTokenException("Unknown verification token"));
        user.verifyEmail(now);
        users.save(user);
    }
}

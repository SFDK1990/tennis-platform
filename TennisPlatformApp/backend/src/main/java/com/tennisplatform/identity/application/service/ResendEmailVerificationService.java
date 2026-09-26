package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.ResendEmailVerification;
import com.tennisplatform.identity.application.port.out.EmailVerificationTokens;
import com.tennisplatform.identity.application.port.out.IdentityMailer;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Earlier links stay valid until they expire: whichever email the student opens first works.
 * Invalidating them would only punish opening the older one.
 */
public class ResendEmailVerificationService implements ResendEmailVerification {

    private final UserRepository users;
    private final VerificationEmails verificationEmails;
    private final Clock clock;

    public ResendEmailVerificationService(UserRepository users, EmailVerificationTokens verificationTokens,
                                          SecureTokenGenerator tokenGenerator, TokenHasher tokenHasher,
                                          IdentityMailer mailer, Clock clock, Duration verificationTokenTtl) {
        this.users = users;
        this.verificationEmails = new VerificationEmails(verificationTokens, tokenGenerator, tokenHasher,
                mailer, verificationTokenTtl);
        this.clock = clock;
    }

    @Override
    @Transactional
    public void resend(UUID userId) {
        users.findById(userId)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> verificationEmails.send(user, clock.instant()));
    }
}

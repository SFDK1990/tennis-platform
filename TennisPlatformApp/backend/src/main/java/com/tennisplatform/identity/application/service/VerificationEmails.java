package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.out.EmailVerificationTokens;
import com.tennisplatform.identity.application.port.out.IdentityMailer;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.domain.OneTimeToken;
import com.tennisplatform.identity.domain.User;

import java.time.Duration;
import java.time.Instant;

/** How a verification link is made and sent, for registering and for asking again. */
final class VerificationEmails {

    private final EmailVerificationTokens verificationTokens;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final IdentityMailer mailer;
    private final Duration ttl;

    VerificationEmails(EmailVerificationTokens verificationTokens, SecureTokenGenerator tokenGenerator,
                       TokenHasher tokenHasher, IdentityMailer mailer, Duration ttl) {
        this.verificationTokens = verificationTokens;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.mailer = mailer;
        this.ttl = ttl;
    }

    /** Only the hash is stored: the raw token exists in the email and nowhere else. */
    void send(User user, Instant now) {
        String rawToken = tokenGenerator.generate();
        verificationTokens.save(OneTimeToken.issue(user.id(), tokenHasher.hash(rawToken), now.plus(ttl)));
        mailer.sendEmailVerification(user.email(), rawToken);
    }
}

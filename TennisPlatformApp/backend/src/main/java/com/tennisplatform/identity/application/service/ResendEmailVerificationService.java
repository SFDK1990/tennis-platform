package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.ResendEmailVerification;
import com.tennisplatform.identity.application.port.out.EmailVerificationTokens;
import com.tennisplatform.identity.application.port.out.IdentityMailer;
import com.tennisplatform.identity.application.port.out.MailCooldown;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Earlier links stay valid until they expire: whichever email the student opens first works.
 * Invalidating them would only punish opening the older one.
 */
public class ResendEmailVerificationService implements ResendEmailVerification {

    private final UserRepository users;
    private final VerificationEmails verificationEmails;
    private final MailCooldown cooldown;
    private final Clock clock;

    public ResendEmailVerificationService(UserRepository users, EmailVerificationTokens verificationTokens,
                                          SecureTokenGenerator tokenGenerator, TokenHasher tokenHasher,
                                          IdentityMailer mailer, MailCooldown cooldown, Clock clock,
                                          Duration verificationTokenTtl) {
        this.users = users;
        this.verificationEmails = new VerificationEmails(verificationTokens, tokenGenerator, tokenHasher,
                mailer, verificationTokenTtl);
        this.cooldown = cooldown;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void resend(UUID userId) {
        // Only resends count, not the email sent at registration: someone who asks for another
        // link straight away because the first never arrived still gets it.
        Instant now = clock.instant();
        users.findById(userId)
                .filter(user -> !user.isEmailVerified())
                .filter(user -> cooldown.tryStart(MailCooldown.Kind.VERIFICATION_RESEND, user.email(), now))
                .ifPresent(user -> verificationEmails.send(user, now));
    }
}

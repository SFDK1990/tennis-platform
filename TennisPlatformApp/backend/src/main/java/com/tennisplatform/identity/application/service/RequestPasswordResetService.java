package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.RequestPasswordReset;
import com.tennisplatform.identity.application.port.out.IdentityMailer;
import com.tennisplatform.identity.application.port.out.MailCooldown;
import com.tennisplatform.identity.application.port.out.PasswordResetTokens;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.OneTimeToken;
import com.tennisplatform.identity.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class RequestPasswordResetService implements RequestPasswordReset {

    private final UserRepository users;
    private final PasswordResetTokens resetTokens;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final IdentityMailer mailer;
    private final MailCooldown cooldown;
    private final Clock clock;
    private final Duration resetTokenTtl;

    public RequestPasswordResetService(UserRepository users, PasswordResetTokens resetTokens,
                                       SecureTokenGenerator tokenGenerator, TokenHasher tokenHasher,
                                       IdentityMailer mailer, MailCooldown cooldown, Clock clock,
                                       Duration resetTokenTtl) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.mailer = mailer;
        this.cooldown = cooldown;
        this.clock = clock;
        this.resetTokenTtl = resetTokenTtl;
    }

    /** Returns normally whether or not the address exists; only a real account gets an email. */
    @Override
    @Transactional
    public void request(Command command) {
        Optional<User> found = parseEmail(command.email()).flatMap(users::findByEmail);
        if (found.isEmpty()) {
            return;
        }

        User user = found.get();
        if (!user.canAuthenticate()) {
            return;
        }

        Instant now = clock.instant();
        if (!cooldown.tryStart(MailCooldown.Kind.PASSWORD_RESET, user.email(), now)) {
            return;
        }
        String rawToken = tokenGenerator.generate();
        resetTokens.save(OneTimeToken.issue(
                user.id(), tokenHasher.hash(rawToken), now.plus(resetTokenTtl)));
        mailer.sendPasswordReset(user.email(), rawToken);
    }

    private Optional<EmailAddress> parseEmail(String raw) {
        try {
            return Optional.of(new EmailAddress(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

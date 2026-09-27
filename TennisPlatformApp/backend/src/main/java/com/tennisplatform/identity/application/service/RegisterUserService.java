package com.tennisplatform.identity.application.service;

import com.tennisplatform.identity.application.port.in.RegisterUser;
import com.tennisplatform.identity.application.port.out.EmailVerificationTokens;
import com.tennisplatform.identity.application.port.out.IdentityMailer;
import com.tennisplatform.identity.application.port.out.MailCooldown;
import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.PasswordPolicy;
import com.tennisplatform.identity.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class RegisterUserService implements RegisterUser {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final VerificationEmails verificationEmails;
    private final IdentityMailer mailer;
    private final MailCooldown cooldown;
    private final Clock clock;

    public RegisterUserService(UserRepository users, EmailVerificationTokens verificationTokens,
                               PasswordHasher passwordHasher, SecureTokenGenerator tokenGenerator,
                               TokenHasher tokenHasher, IdentityMailer mailer, MailCooldown cooldown, Clock clock,
                               Duration verificationTokenTtl) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.verificationEmails = new VerificationEmails(verificationTokens, tokenGenerator, tokenHasher,
                mailer, verificationTokenTtl);
        this.mailer = mailer;
        this.cooldown = cooldown;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void register(Command command) {
        PasswordPolicy.validate(command.password());
        EmailAddress email = new EmailAddress(command.email());
        Instant now = clock.instant();

        Optional<User> existing = users.findByEmail(email);
        if (existing.isPresent()) {
            // Nothing is created and nothing is reported back. The real owner is told instead,
            // so the person asking learns nothing about whether the account exists.
            if (cooldown.tryStart(MailCooldown.Kind.EXISTING_ACCOUNT_WARNING, email, now)) {
                mailer.sendRegistrationAttemptOnExistingAccount(email);
            }
            return;
        }

        User user = users.save(User.register(email, passwordHasher.hash(command.password()), now));
        verificationEmails.send(user, now);
    }
}

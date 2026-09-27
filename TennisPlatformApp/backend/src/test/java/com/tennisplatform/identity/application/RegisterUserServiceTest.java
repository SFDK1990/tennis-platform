package com.tennisplatform.identity.application;

import com.tennisplatform.identity.application.port.in.RegisterUser;
import com.tennisplatform.identity.application.port.out.EmailVerificationTokens;
import com.tennisplatform.identity.application.port.out.IdentityMailer;
import com.tennisplatform.identity.application.port.out.MailCooldown;
import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.application.service.RegisterUserService;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.User;
import com.tennisplatform.identity.domain.WeakPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final String VALID_PASSWORD = "a-valid-password";

    private UserRepository users;
    private EmailVerificationTokens verificationTokens;
    private IdentityMailer mailer;
    private MailCooldown cooldown;
    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        verificationTokens = mock(EmailVerificationTokens.class);
        mailer = mock(IdentityMailer.class);
        cooldown = mock(MailCooldown.class);
        when(cooldown.tryStart(any(), any(), any())).thenReturn(true);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);
        SecureTokenGenerator generator = mock(SecureTokenGenerator.class);
        TokenHasher hasher = mock(TokenHasher.class);

        when(passwordHasher.hash(any())).thenReturn("hashed-password");
        when(generator.generate()).thenReturn("raw-verification-token");
        when(hasher.hash(any())).thenReturn("hashed-token");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));

        service = new RegisterUserService(users, verificationTokens, passwordHasher, generator,
                hasher, mailer, cooldown, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(1));
    }

    @Test
    void createsTheAccountAndSendsAVerificationEmail() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());

        service.register(new RegisterUser.Command("new@example.com", VALID_PASSWORD));

        verify(users).save(any(User.class));
        verify(verificationTokens).save(any());
        verify(mailer).sendEmailVerification(new EmailAddress("new@example.com"), "raw-verification-token");
    }

    /**
     * Registering with an address that already exists must be indistinguishable from a fresh
     * registration: no exception, no different outcome. The real owner is notified instead, so
     * they learn about the attempt while the requester learns nothing.
     */
    @Test
    void anExistingEmailCreatesNothingAndNotifiesTheRealOwner() {
        User existing = User.register(new EmailAddress("taken@example.com"), "hash", NOW);
        when(users.findByEmail(any())).thenReturn(Optional.of(existing));

        assertThatCode(() -> service.register(new RegisterUser.Command("taken@example.com", VALID_PASSWORD)))
                .doesNotThrowAnyException();

        verify(users, never()).save(any());
        verify(verificationTokens, never()).save(any());
        verify(mailer, never()).sendEmailVerification(any(), any());
        verify(mailer).sendRegistrationAttemptOnExistingAccount(new EmailAddress("taken@example.com"));
    }

    /** Otherwise registering again and again floods the real owner's inbox. */
    @Test
    void theOwnerIsNotWarnedAgainWhileTheLastWarningIsRecent() {
        User existing = User.register(new EmailAddress("taken@example.com"), "hash", NOW);
        when(users.findByEmail(any())).thenReturn(Optional.of(existing));
        when(cooldown.tryStart(any(), any(), any())).thenReturn(false);

        assertThatCode(() -> service.register(new RegisterUser.Command("taken@example.com", VALID_PASSWORD)))
                .doesNotThrowAnyException();

        verify(mailer, never()).sendRegistrationAttemptOnExistingAccount(any());
    }

    @Test
    void rejectsAPasswordThatFailsThePolicy() {
        assertThatThrownBy(() -> service.register(new RegisterUser.Command("new@example.com", "short")))
                .isInstanceOf(WeakPasswordException.class);

        verify(users, never()).save(any());
    }
}

package com.tennisplatform.identity.application;

import com.tennisplatform.identity.application.port.in.Login;
import com.tennisplatform.identity.application.port.out.AccessTokenIssuer;
import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.application.service.LoginService;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.InvalidCredentialsException;
import com.tennisplatform.identity.domain.Role;
import com.tennisplatform.identity.domain.User;
import com.tennisplatform.identity.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private UserRepository users;
    private PasswordHasher passwordHasher;
    private LoginService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        RefreshTokens refreshTokens = mock(RefreshTokens.class);
        SecureTokenGenerator generator = mock(SecureTokenGenerator.class);
        TokenHasher hasher = mock(TokenHasher.class);
        AccessTokenIssuer issuer = mock(AccessTokenIssuer.class);

        when(generator.generate()).thenReturn("raw-refresh");
        when(hasher.hash(any())).thenReturn("hashed");
        when(issuer.issue(any())).thenReturn("access-token");
        when(issuer.lifetime()).thenReturn(Duration.ofMinutes(15));
        when(refreshTokens.save(any())).thenAnswer(call -> call.getArgument(0));

        user = User.rehydrate(UUID.randomUUID(), new EmailAddress("s@example.com"), "stored-hash",
                Role.STUDENT, UserStatus.ACTIVE, NOW, NOW);

        service = new LoginService(users, refreshTokens, passwordHasher, generator, hasher, issuer,
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(14));
    }

    @Test
    void issuesASessionOnCorrectCredentials() {
        when(users.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret", "stored-hash")).thenReturn(true);

        var result = service.login(new Login.Command("s@example.com", "secret"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh");
        assertThat(result.user().email()).isEqualTo("s@example.com");
    }

    /**
     * Without this, a login against an unknown email would return noticeably faster than one
     * against a real account, and the response time alone would reveal which emails exist.
     */
    @Test
    void stillHashesWhenTheEmailIsUnknown() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new Login.Command("ghost@example.com", "secret")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordHasher).burnTime("secret");
    }

    @Test
    void reportsTheSameFailureForAWrongPassword() {
        when(users.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordHasher.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new Login.Command("s@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    /** Saying "disabled" would confirm that the address has an account. */
    @Test
    void reportsTheSameFailureForADisabledAccount() {
        user.disable();
        when(users.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new Login.Command("s@example.com", "secret")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    /** A malformed address is a failed login, not a validation error - same answer as any other. */
    @Test
    void reportsTheSameFailureForAMalformedEmail() {
        assertThatThrownBy(() -> service.login(new Login.Command("not-an-email", "secret")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void anUnverifiedAccountCanStillSignIn() {
        User unverified = User.register(new EmailAddress("new@example.com"), "stored-hash", NOW);
        when(users.findByEmail(any())).thenReturn(Optional.of(unverified));
        when(passwordHasher.matches("secret", "stored-hash")).thenReturn(true);

        var result = service.login(new Login.Command("new@example.com", "secret"));

        // The summary carries the status as a string, so that a module outside identity can
        // read it without importing identity's domain enum.
        assertThat(result.user().status()).isEqualTo(UserStatus.PENDING_VERIFICATION.name());
    }
}

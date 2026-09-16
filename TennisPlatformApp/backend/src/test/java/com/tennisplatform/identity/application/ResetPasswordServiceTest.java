package com.tennisplatform.identity.application;

import com.tennisplatform.identity.application.port.in.ResetPassword;
import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.PasswordResetTokens;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.application.service.ResetPasswordService;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.InvalidTokenException;
import com.tennisplatform.identity.domain.OneTimeToken;
import com.tennisplatform.identity.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResetPasswordServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final String NEW_PASSWORD = "a-brand-new-password";

    private PasswordResetTokens resetTokens;
    private UserRepository users;
    private RefreshTokens refreshTokens;
    private ResetPasswordService service;
    private User user;

    @BeforeEach
    void setUp() {
        resetTokens = mock(PasswordResetTokens.class);
        users = mock(UserRepository.class);
        refreshTokens = mock(RefreshTokens.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);
        TokenHasher tokenHasher = mock(TokenHasher.class);

        when(tokenHasher.hash(any())).thenAnswer(call -> "hashed:" + call.getArgument(0));
        when(passwordHasher.hash(any())).thenReturn("new-hash");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));

        user = User.register(new EmailAddress("s@example.com"), "old-hash", NOW);
        when(users.findById(user.id())).thenReturn(Optional.of(user));

        service = new ResetPasswordService(resetTokens, users, refreshTokens, passwordHasher,
                tokenHasher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * A reset usually means the account may be compromised. Leaving previously issued sessions
     * alive would defeat the entire point of changing the password.
     */
    @Test
    void resettingRevokesEveryExistingSession() {
        OneTimeToken token = OneTimeToken.issue(user.id(), "hashed:valid", NOW.plusSeconds(3600));
        when(resetTokens.findByTokenHash("hashed:valid")).thenReturn(Optional.of(token));

        service.reset(new ResetPassword.Command("valid", NEW_PASSWORD));

        verify(refreshTokens).revokeAllForUser(user.id(), NOW);
        verify(users).save(user);
    }

    @Test
    void theTokenCannotBeUsedTwice() {
        OneTimeToken token = OneTimeToken.issue(user.id(), "hashed:valid", NOW.plusSeconds(3600));
        token.consume(NOW);
        when(resetTokens.findByTokenHash("hashed:valid")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.reset(new ResetPassword.Command("valid", NEW_PASSWORD)))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void anExpiredTokenIsRejectedAndNothingChanges() {
        OneTimeToken expired = OneTimeToken.issue(user.id(), "hashed:old", NOW.minusSeconds(1));
        when(resetTokens.findByTokenHash("hashed:old")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.reset(new ResetPassword.Command("old", NEW_PASSWORD)))
                .isInstanceOf(InvalidTokenException.class);

        verify(users, never()).save(any());
        verify(refreshTokens, never()).revokeAllForUser(any(), any());
    }

    @Test
    void anUnknownTokenIsRejected() {
        when(resetTokens.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reset(new ResetPassword.Command("ghost", NEW_PASSWORD)))
                .isInstanceOf(InvalidTokenException.class);
    }
}

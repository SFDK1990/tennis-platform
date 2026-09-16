package com.tennisplatform.identity.application;

import com.tennisplatform.identity.application.port.out.AccessTokenIssuer;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.application.port.in.RefreshSession;
import com.tennisplatform.identity.application.service.RefreshSessionService;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.InvalidTokenException;
import com.tennisplatform.identity.domain.RefreshToken;
import com.tennisplatform.identity.domain.Role;
import com.tennisplatform.identity.domain.TokenReuseDetectedException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofDays(14);

    private RefreshTokens refreshTokens;
    private UserRepository users;
    private RefreshSessionService service;
    private User user;

    @BeforeEach
    void setUp() {
        refreshTokens = mock(RefreshTokens.class);
        users = mock(UserRepository.class);
        SecureTokenGenerator generator = mock(SecureTokenGenerator.class);
        TokenHasher hasher = mock(TokenHasher.class);
        AccessTokenIssuer issuer = mock(AccessTokenIssuer.class);

        when(generator.generate()).thenReturn("new-raw-token");
        when(hasher.hash(any())).thenAnswer(call -> "hashed:" + call.getArgument(0));
        when(issuer.issue(any())).thenReturn("access-token");
        when(issuer.lifetime()).thenReturn(Duration.ofMinutes(15));
        when(refreshTokens.save(any())).thenAnswer(call -> call.getArgument(0));

        user = User.rehydrate(UUID.randomUUID(), new EmailAddress("s@example.com"), "hash",
                Role.STUDENT, UserStatus.ACTIVE, NOW, NOW);
        when(users.findById(user.id())).thenReturn(Optional.of(user));

        service = new RefreshSessionService(refreshTokens, users, generator, hasher, issuer,
                Clock.fixed(NOW, ZoneOffset.UTC), TTL);
    }

    @Test
    void rotatesAValidTokenAndIssuesANewOne() {
        RefreshToken current = RefreshToken.startFamily(user.id(), "hashed:presented", NOW, NOW.plus(TTL));
        when(refreshTokens.findByTokenHash("hashed:presented")).thenReturn(Optional.of(current));

        var result = service.refresh(new RefreshSession.Command("presented"));

        assertThat(result.refreshToken()).isEqualTo("new-raw-token");
        assertThat(current.wasAlreadyRotated()).isTrue();
    }

    /**
     * The core anti-theft rule: an already rotated token means two parties hold it, and there
     * is no way to know which one is calling, so the entire chain dies.
     */
    @Test
    void reusingAnAlreadyRotatedTokenRevokesTheWholeFamily() {
        RefreshToken stolen = RefreshToken.startFamily(user.id(), "hashed:stolen", NOW, NOW.plus(TTL));
        RefreshToken successor = RefreshToken.continueFamily(user.id(), "hashed:next",
                stolen.familyId(), NOW, NOW.plus(TTL));
        stolen.replaceWith(successor, NOW);
        when(refreshTokens.findByTokenHash("hashed:stolen")).thenReturn(Optional.of(stolen));

        assertThatThrownBy(() -> service.refresh(new RefreshSession.Command("stolen")))
                .isInstanceOf(TokenReuseDetectedException.class);

        verify(refreshTokens).revokeFamily(stolen.familyId(), NOW);
    }

    @Test
    void anUnknownTokenIsRejectedWithoutRevokingAnything() {
        when(refreshTokens.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(new RefreshSession.Command("whatever")))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokens, never()).revokeFamily(any(), any());
    }

    @Test
    void anExpiredTokenIsRejected() {
        RefreshToken expired = RefreshToken.startFamily(user.id(), "hashed:old",
                NOW.minus(Duration.ofDays(30)), NOW.minusSeconds(1));
        when(refreshTokens.findByTokenHash("hashed:old")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.refresh(new RefreshSession.Command("old")))
                .isInstanceOf(InvalidTokenException.class);
    }

    /** Disabling an account must not leave its existing sessions rolling on until they expire. */
    @Test
    void aDisabledAccountCannotRefreshAndItsFamilyIsRevoked() {
        user.disable();
        RefreshToken current = RefreshToken.startFamily(user.id(), "hashed:presented", NOW, NOW.plus(TTL));
        when(refreshTokens.findByTokenHash("hashed:presented")).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.refresh(new RefreshSession.Command("presented")))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokens).revokeFamily(eq(current.familyId()), any());
    }
}

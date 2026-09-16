package com.tennisplatform.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final EmailAddress EMAIL = new EmailAddress("student@example.com");

    @Test
    void publicRegistrationAlwaysProducesAnUnverifiedStudent() {
        User user = User.register(EMAIL, "hash", NOW);

        assertThat(user.role()).isEqualTo(Role.STUDENT);
        assertThat(user.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(user.isEmailVerified()).isFalse();
    }

    /**
     * Verification gates booking, not access: a lost verification email must not lock the
     * user out of the application entirely.
     */
    @Test
    void anUnverifiedAccountMayStillAuthenticate() {
        assertThat(User.register(EMAIL, "hash", NOW).canAuthenticate()).isTrue();
    }

    @Test
    void aDisabledAccountMayNotAuthenticate() {
        User user = User.register(EMAIL, "hash", NOW);
        user.disable();

        assertThat(user.canAuthenticate()).isFalse();
    }

    @Test
    void verifyingActivatesTheAccountAndStampsTheMoment() {
        User user = User.register(EMAIL, "hash", NOW);
        user.verifyEmail(NOW);

        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.emailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    void verifyingTwiceKeepsTheOriginalTimestamp() {
        User user = User.register(EMAIL, "hash", NOW);
        user.verifyEmail(NOW);
        user.verifyEmail(NOW.plusSeconds(3600));

        assertThat(user.emailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    void aDisabledAccountCannotBeVerifiedOrChangeItsPassword() {
        User user = User.register(EMAIL, "hash", NOW);
        user.disable();

        assertThatThrownBy(() -> user.verifyEmail(NOW)).isInstanceOf(AccountNotActiveException.class);
        assertThatThrownBy(() -> user.changePassword("new")).isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void bootstrappedTeacherIsCreatedAlreadyActive() {
        User teacher = User.bootstrapTeacher(EMAIL, "hash", NOW);

        assertThat(teacher.role()).isEqualTo(Role.TEACHER);
        assertThat(teacher.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(teacher.isEmailVerified()).isTrue();
    }
}

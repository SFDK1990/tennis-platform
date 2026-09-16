package com.tennisplatform.identity.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsAPasswordAtTheMinimumLength() {
        assertThatCode(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MIN_LENGTH)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsShortPasswords() {
        assertThatThrownBy(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MIN_LENGTH - 1)))
                .isInstanceOf(WeakPasswordException.class);
    }

    /**
     * BCrypt ignores everything past 72 bytes. Truncating silently would make two different
     * long passwords interchangeable, so the policy rejects instead.
     */
    @Test
    void rejectsPasswordsBeyondWhatBcryptCanHash() {
        assertThatThrownBy(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MAX_BYTES + 1)))
                .isInstanceOf(WeakPasswordException.class);
    }

    /** Multi-byte characters count as bytes, not as characters. */
    @Test
    void countsBytesNotCharacters() {
        assertThatThrownBy(() -> PasswordPolicy.validate("ñ".repeat(37)))
                .isInstanceOf(WeakPasswordException.class);
    }
}

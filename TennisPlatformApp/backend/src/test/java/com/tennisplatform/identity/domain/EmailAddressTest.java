package com.tennisplatform.identity.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressTest {

    @Test
    void normalizesCaseAndSurroundingWhitespace() {
        assertThat(new EmailAddress("  Daniel@Example.COM ").value()).isEqualTo("daniel@example.com");
    }

    /**
     * The unique index on users.email is case sensitive, so without normalization these two
     * would become two separate accounts for the same person.
     */
    @Test
    void treatsDifferentCasingAsTheSameAddress() {
        assertThat(new EmailAddress("A@B.com")).isEqualTo(new EmailAddress("a@b.com"));
    }

    @Test
    void rejectsMalformedAddresses() {
        assertThatThrownBy(() -> new EmailAddress("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmailAddress("two@@at.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmailAddress(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

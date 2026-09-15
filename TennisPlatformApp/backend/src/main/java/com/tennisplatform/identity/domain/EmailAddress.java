package com.tennisplatform.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalized email address. Normalization happens here, at construction, rather than at
 * each call site: the unique index on users.email is case sensitive, so "A@b.com" and
 * "a@b.com" would otherwise be two accounts.
 */
public record EmailAddress(String value) {

    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_LENGTH = 255;

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Email exceeds " + MAX_LENGTH + " characters");
        }
        if (!SHAPE.matcher(value).matches()) {
            throw new IllegalArgumentException("Email format is invalid");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

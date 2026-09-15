package com.tennisplatform.identity.domain;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    /** 10, not 8: the API contract (openapi.yaml, RegisterRequest) already promises this. */
    public static final int MIN_LENGTH = 10;

    /**
     * BCrypt silently ignores anything past 72 bytes, which would make two different long
     * passwords interchangeable. Rejecting is safer than truncating without telling anyone.
     */
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new WeakPasswordException("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new WeakPasswordException("Password must not exceed " + MAX_BYTES + " bytes");
        }
    }
}

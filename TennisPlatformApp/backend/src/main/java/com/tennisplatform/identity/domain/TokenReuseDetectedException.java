package com.tennisplatform.identity.domain;

import java.util.UUID;

/**
 * Raised when an already-rotated refresh token is presented. Distinct from
 * {@link InvalidTokenException} because it carries a security consequence: the whole
 * family must be revoked. The client still sees a plain 401.
 */
public class TokenReuseDetectedException extends RuntimeException {

    private final UUID familyId;

    public TokenReuseDetectedException(UUID familyId) {
        super("Refresh token reuse detected");
        this.familyId = familyId;
    }

    public UUID familyId() {
        return familyId;
    }
}

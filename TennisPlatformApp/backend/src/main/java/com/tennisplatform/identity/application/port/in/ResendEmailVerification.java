package com.tennisplatform.identity.application.port.in;

import java.util.UUID;

/**
 * A new verification link for a signed-in user whose link expired or never arrived. Without
 * it, a student with a dead link could log in but never book.
 */
public interface ResendEmailVerification {

    /** Does nothing for an address that is already verified: there is nothing left to confirm. */
    void resend(UUID userId);
}

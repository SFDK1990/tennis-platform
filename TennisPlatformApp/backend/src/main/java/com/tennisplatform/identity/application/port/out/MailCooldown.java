package com.tennisplatform.identity.application.port.out;

import com.tennisplatform.identity.domain.EmailAddress;

import java.time.Instant;

/**
 * At most one email of each kind to the same address per cooldown. Without it, anyone could
 * send a stranger a reset email per request and have our domain flagged as a spam source
 * (26-fase15-analisis-seguridad.md). Callers answer the same either way, so a suppressed email
 * reveals nothing.
 */
public interface MailCooldown {

    enum Kind { PASSWORD_RESET, EXISTING_ACCOUNT_WARNING, VERIFICATION_RESEND }

    /** True, and the cooldown starts, if this kind of email may go to this address now. */
    boolean tryStart(Kind kind, EmailAddress recipient, Instant now);
}

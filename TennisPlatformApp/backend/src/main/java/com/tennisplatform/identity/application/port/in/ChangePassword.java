package com.tennisplatform.identity.application.port.in;

import java.util.UUID;

/**
 * A signed-in user changes their own password. It asks for the current one, so a stolen access
 * token is not enough to take the account over, and it ends every other session while keeping
 * the caller's: whoever changes it because they suspect something throws the others out, not
 * themselves (30-fase19-analisis-cierre-mvp.md).
 */
public interface ChangePassword {

    /** The new session of the caller; every other one has been revoked. */
    AuthenticationResult change(Command command);

    record Command(UUID userId, String currentPassword, String newPassword) {
    }
}

package com.tennisplatform.identity.application.port.in;

public interface Logout {

    /** Idempotent: an unknown or already revoked token is not an error. */
    void logout(Command command);

    record Command(String refreshToken) {
    }
}

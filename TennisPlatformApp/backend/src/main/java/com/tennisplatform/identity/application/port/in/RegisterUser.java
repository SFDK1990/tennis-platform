package com.tennisplatform.identity.application.port.in;

public interface RegisterUser {

    /**
     * Returns nothing and never reports that the email was taken: the caller must not be
     * able to distinguish a fresh registration from an existing account.
     */
    void register(Command command);

    record Command(String email, String password) {
    }
}

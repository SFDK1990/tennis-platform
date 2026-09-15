package com.tennisplatform.identity.application.port.out;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);

    /**
     * Runs the same work as a real verification and always fails. Called when the email is
     * unknown so that login takes comparable time whether or not the account exists -
     * otherwise response timing alone reveals which emails are registered.
     */
    void burnTime(String rawPassword);
}

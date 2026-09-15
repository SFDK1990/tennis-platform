package com.tennisplatform.identity.domain;

/**
 * Deliberately says nothing about which half was wrong, and is thrown for an unknown email
 * too, so responses cannot be used to discover which accounts exist.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}

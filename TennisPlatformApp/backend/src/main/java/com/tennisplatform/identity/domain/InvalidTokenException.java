package com.tennisplatform.identity.domain;

/**
 * Covers unknown, expired and already-used tokens alike. The caller must not tell them
 * apart in its response: which one it was is itself information an attacker can use.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}

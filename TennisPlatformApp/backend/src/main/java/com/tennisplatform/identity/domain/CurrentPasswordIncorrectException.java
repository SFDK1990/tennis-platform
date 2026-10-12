package com.tennisplatform.identity.domain;

/**
 * The current password given to change it does not match. Unlike a failed login it can say so:
 * the caller is already authenticated, so it reveals nothing about which accounts exist.
 */
public class CurrentPasswordIncorrectException extends RuntimeException {

    public CurrentPasswordIncorrectException() {
        super("The current password is incorrect");
    }
}

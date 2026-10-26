package com.tennisplatform.notification.domain;

/** The message breaks a rule: no subject, too long, or an audience without what it needs. Maps to 400. */
public class InvalidMessageException extends RuntimeException {

    public InvalidMessageException(String message) {
        super(message);
    }
}

package com.tennisplatform.notification.domain;

/** Nobody is in the audience: a message that reaches nobody is almost certainly a mistake. Maps to 422. */
public class NoRecipientsException extends RuntimeException {

    public NoRecipientsException(String message) {
        super(message);
    }
}

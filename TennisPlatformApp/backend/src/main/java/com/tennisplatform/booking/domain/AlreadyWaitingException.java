package com.tennisplatform.booking.domain;

/** The student is already on this lesson's waiting list. */
public class AlreadyWaitingException extends RuntimeException {

    public AlreadyWaitingException(String message) {
        super(message);
    }
}

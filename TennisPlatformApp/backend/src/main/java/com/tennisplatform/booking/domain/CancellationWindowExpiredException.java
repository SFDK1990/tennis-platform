package com.tennisplatform.booking.domain;

/** The student tried to cancel with less than 24 hours of notice. */
public class CancellationWindowExpiredException extends RuntimeException {

    public CancellationWindowExpiredException(String message) {
        super(message);
    }
}

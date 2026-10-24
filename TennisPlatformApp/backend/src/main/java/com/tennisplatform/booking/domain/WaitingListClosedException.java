package com.tennisplatform.booking.domain;

/** The lesson starts in less than 24 hours, and its waiting list is closed. */
public class WaitingListClosedException extends RuntimeException {

    public WaitingListClosedException(String message) {
        super(message);
    }
}

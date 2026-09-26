package com.tennisplatform.booking.domain;

/** The booking no longer stands, so it can be neither cancelled again nor marked. */
public class BookingAlreadyCancelledException extends RuntimeException {

    public BookingAlreadyCancelledException(String message) {
        super(message);
    }
}

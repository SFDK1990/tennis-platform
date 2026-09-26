package com.tennisplatform.booking.domain;

/** The student already holds a confirmed seat in this lesson. */
public class BookingAlreadyExistsException extends RuntimeException {

    public BookingAlreadyExistsException(String message) {
        super(message);
    }
}

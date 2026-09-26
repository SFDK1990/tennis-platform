package com.tennisplatform.booking.domain;

/**
 * No booking with that id - or none the caller may see, which is answered the same way on
 * purpose: confirming that somebody else's booking exists is telling the caller something
 * they had no way to know.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String message) {
        super(message);
    }
}

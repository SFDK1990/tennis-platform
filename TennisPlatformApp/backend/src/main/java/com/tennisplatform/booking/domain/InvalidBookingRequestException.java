package com.tennisplatform.booking.domain;

/**
 * A request that is well-formed JSON but asks for something that does not exist: an attendance
 * value other than ATTENDED or NO_SHOW, an unknown status filter, the same booking twice in one
 * batch. Mapped to the same {@code VALIDATION_ERROR} the bean validation failures use.
 */
public class InvalidBookingRequestException extends RuntimeException {

    public InvalidBookingRequestException(String message) {
        super(message);
    }
}

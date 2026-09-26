package com.tennisplatform.booking.domain;

/**
 * The lesson has started, so it can no longer be booked and its bookings can no longer be
 * cancelled. A 422: re-reading will not make the clock go back.
 */
public class LessonAlreadyStartedException extends RuntimeException {

    public LessonAlreadyStartedException(String message) {
        super(message);
    }
}

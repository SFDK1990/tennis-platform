package com.tennisplatform.lesson.domain;

/**
 * Fewer seats than students already booked. A 409 and not a 422: if one of them cancels, asking
 * again may succeed.
 */
public class LessonCapacityBelowBookingsException extends RuntimeException {

    public LessonCapacityBelowBookingsException(String message) {
        super(message);
    }
}

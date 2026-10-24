package com.tennisplatform.booking.domain;

/** A seat is free: the student books it instead of waiting. */
public class LessonNotFullException extends RuntimeException {

    public LessonNotFullException(String message) {
        super(message);
    }
}

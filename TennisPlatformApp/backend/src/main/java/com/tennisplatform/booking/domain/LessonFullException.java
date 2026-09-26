package com.tennisplatform.booking.domain;

/** Every seat of the lesson is taken; counted while holding the lesson's lock. */
public class LessonFullException extends RuntimeException {

    public LessonFullException(String message) {
        super(message);
    }
}

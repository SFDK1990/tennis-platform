package com.tennisplatform.booking.domain;

/** The lesson was cancelled. A 409: the screen that offered it is out of date. */
public class LessonNotBookableException extends RuntimeException {

    public LessonNotBookableException(String message) {
        super(message);
    }
}

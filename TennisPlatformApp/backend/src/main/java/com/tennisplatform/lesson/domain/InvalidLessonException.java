package com.tennisplatform.lesson.domain;

/** A lesson that cannot exist: a duration that is not a multiple of 30, one that crosses midnight, a capacity that contradicts the type. */
public class InvalidLessonException extends RuntimeException {

    public InvalidLessonException(String message) {
        super(message);
    }
}

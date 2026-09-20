package com.tennisplatform.lesson.domain;

/** The lesson was already cancelled. Almost always a stale screen rather than a real attempt. */
public class LessonAlreadyCancelledException extends RuntimeException {

    public LessonAlreadyCancelledException(String message) {
        super(message);
    }
}

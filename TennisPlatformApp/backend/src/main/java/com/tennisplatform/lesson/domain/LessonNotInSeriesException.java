package com.tennisplatform.lesson.domain;

/** The lesson was created on its own, so it has no following lessons to cancel with it. */
public class LessonNotInSeriesException extends RuntimeException {

    public LessonNotInSeriesException(String message) {
        super(message);
    }
}

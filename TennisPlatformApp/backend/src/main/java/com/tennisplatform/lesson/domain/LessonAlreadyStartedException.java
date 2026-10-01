package com.tennisplatform.lesson.domain;

/** The lesson has started; what it is can no longer change. Re-reading does not turn back the clock. */
public class LessonAlreadyStartedException extends RuntimeException {

    public LessonAlreadyStartedException(String message) {
        super(message);
    }
}

package com.tennisplatform.lesson.domain;

/** The requested listing spans more days than a single read is allowed to cover. */
public class LessonRangeTooWideException extends RuntimeException {

    public LessonRangeTooWideException(String message) {
        super(message);
    }
}

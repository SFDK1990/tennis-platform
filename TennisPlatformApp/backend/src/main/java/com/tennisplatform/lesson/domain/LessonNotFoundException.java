package com.tennisplatform.lesson.domain;

/** No lesson with that id. */
public class LessonNotFoundException extends RuntimeException {

    public LessonNotFoundException(String message) {
        super(message);
    }
}

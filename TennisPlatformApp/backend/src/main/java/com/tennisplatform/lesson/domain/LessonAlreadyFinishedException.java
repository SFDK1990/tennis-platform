package com.tennisplatform.lesson.domain;

/**
 * The lesson already ended, so it cannot be cancelled.
 *
 * <p>Cancelling something that already happened is rewriting the past: the students either came
 * or did not, and Fase 9 will record which. Re-reading does not change the answer, so this is a
 * business rule and not a conflict.
 */
public class LessonAlreadyFinishedException extends RuntimeException {

    public LessonAlreadyFinishedException(String message) {
        super(message);
    }
}

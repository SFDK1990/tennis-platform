package com.tennisplatform.lesson.domain;

/**
 * The lesson falls outside the teacher's configured availability and the caller did not ask to
 * force it.
 *
 * <p>A business rule rather than a conflict: re-reading the availability will not change the
 * answer. Either the hours change or the teacher forces it on purpose.
 */
public class LessonOutsideAvailabilityException extends RuntimeException {

    public LessonOutsideAvailabilityException(String message) {
        super(message);
    }
}

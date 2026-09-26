package com.tennisplatform.lesson.domain;

/**
 * The lesson would start at or before the moment it is created.
 *
 * <p>Fase 8 left this open for Fase 9 to decide, because only then would it be clear what such a
 * lesson could be for. The answer is nothing: a student cannot book a lesson that has already
 * started, so it can never have bookings, and without bookings there is no attendance to record.
 * What is left is a typing mistake that nobody would otherwise stop.
 */
public class LessonInThePastException extends RuntimeException {

    public LessonInThePastException(String message) {
        super(message);
    }
}

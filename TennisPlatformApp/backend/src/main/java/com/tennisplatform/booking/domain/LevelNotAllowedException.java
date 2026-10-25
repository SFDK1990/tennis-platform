package com.tennisplatform.booking.domain;

/** The lesson is for another level than the student's (Fase 23). Maps to 422. */
public class LevelNotAllowedException extends RuntimeException {

    public LevelNotAllowedException(String message) {
        super(message);
    }
}

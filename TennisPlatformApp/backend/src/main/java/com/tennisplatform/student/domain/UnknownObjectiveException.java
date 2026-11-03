package com.tennisplatform.student.domain;

/**
 * The objective is not a live one of the teacher's levels (or, to unmark, not one of them at all).
 * Maps to 404 {@code OBJECTIVE_NOT_FOUND}, like the teacher module's own.
 */
public class UnknownObjectiveException extends RuntimeException {

    public UnknownObjectiveException(String message) {
        super(message);
    }
}

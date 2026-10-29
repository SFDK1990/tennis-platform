package com.tennisplatform.lesson.domain;

/** Another court already has that name. Maps to 409 COURT_ALREADY_EXISTS. */
public class CourtAlreadyExistsException extends RuntimeException {

    public CourtAlreadyExistsException(String message) {
        super(message);
    }
}

package com.tennisplatform.lesson.domain;

/** No court with that id. Maps to 404 COURT_NOT_FOUND. */
public class CourtNotFoundException extends RuntimeException {

    public CourtNotFoundException(String message) {
        super(message);
    }
}

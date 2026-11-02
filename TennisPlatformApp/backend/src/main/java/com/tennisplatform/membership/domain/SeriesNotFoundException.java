package com.tennisplatform.membership.domain;

/** No weekly series with that id belongs to the teacher. Maps to 404. */
public class SeriesNotFoundException extends RuntimeException {

    public SeriesNotFoundException(String message) {
        super(message);
    }
}

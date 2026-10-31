package com.tennisplatform.membership.domain;

/** No fee with that id belongs to the teacher. Maps to 404. */
public class FeeNotFoundException extends RuntimeException {

    public FeeNotFoundException(String message) {
        super(message);
    }
}

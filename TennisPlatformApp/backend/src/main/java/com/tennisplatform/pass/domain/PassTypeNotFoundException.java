package com.tennisplatform.pass.domain;

/** No pass type with that id belongs to the teacher. Maps to 404. */
public class PassTypeNotFoundException extends RuntimeException {

    public PassTypeNotFoundException(String message) {
        super(message);
    }
}

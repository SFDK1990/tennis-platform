package com.tennisplatform.pass.domain;

/** No pass with that id belongs to the teacher. Maps to 404. */
public class PassNotFoundException extends RuntimeException {

    public PassNotFoundException(String message) {
        super(message);
    }
}

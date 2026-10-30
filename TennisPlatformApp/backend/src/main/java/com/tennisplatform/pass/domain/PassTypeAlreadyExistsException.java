package com.tennisplatform.pass.domain;

/** The teacher already has a pass type with that name. Maps to 409. */
public class PassTypeAlreadyExistsException extends RuntimeException {

    public PassTypeAlreadyExistsException(String message) {
        super(message);
    }
}

package com.tennisplatform.teacher.domain;

/** The level already has a live objective with that title. Maps to 409. */
public class ObjectiveAlreadyExistsException extends RuntimeException {

    public ObjectiveAlreadyExistsException(String message) {
        super(message);
    }
}

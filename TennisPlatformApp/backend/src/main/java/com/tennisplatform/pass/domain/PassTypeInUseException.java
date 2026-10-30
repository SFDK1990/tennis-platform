package com.tennisplatform.pass.domain;

/** A student has a pass of this type, so it can only be retired. Maps to 409. */
public class PassTypeInUseException extends RuntimeException {

    public PassTypeInUseException(String message) {
        super(message);
    }
}

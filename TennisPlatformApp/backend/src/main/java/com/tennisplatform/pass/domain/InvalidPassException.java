package com.tennisplatform.pass.domain;

/** The values of a pass type or a pass break a rule. Maps to 400. */
public class InvalidPassException extends RuntimeException {

    public InvalidPassException(String message) {
        super(message);
    }
}

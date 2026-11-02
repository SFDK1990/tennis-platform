package com.tennisplatform.teacher.domain;

/** The objective breaks a rule of the domain. Maps to 400. */
public class InvalidObjectiveException extends RuntimeException {

    public InvalidObjectiveException(String message) {
        super(message);
    }
}

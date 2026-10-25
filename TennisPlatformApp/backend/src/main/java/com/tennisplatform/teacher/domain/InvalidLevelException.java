package com.tennisplatform.teacher.domain;

/** The level breaks a rule of the domain. Maps to 400. */
public class InvalidLevelException extends RuntimeException {

    public InvalidLevelException(String message) {
        super(message);
    }
}

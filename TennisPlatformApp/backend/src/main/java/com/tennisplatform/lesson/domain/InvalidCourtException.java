package com.tennisplatform.lesson.domain;

/** The court's name is blank or too long. Maps to 400. */
public class InvalidCourtException extends RuntimeException {

    public InvalidCourtException(String message) {
        super(message);
    }
}

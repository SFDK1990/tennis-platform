package com.tennisplatform.teacher.domain;

/** The submitted profile data breaks a rule of the domain. Maps to 400. */
public class InvalidTeacherProfileException extends RuntimeException {

    public InvalidTeacherProfileException(String message) {
        super(message);
    }
}

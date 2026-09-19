package com.tennisplatform.student.domain;

/** The submitted profile data breaks a rule of the domain. Maps to 400. */
public class InvalidStudentProfileException extends RuntimeException {

    public InvalidStudentProfileException(String message) {
        super(message);
    }
}

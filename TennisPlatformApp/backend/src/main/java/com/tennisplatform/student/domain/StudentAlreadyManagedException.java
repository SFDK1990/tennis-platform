package com.tennisplatform.student.domain;

/**
 * The teacher already manages this student. Maps to 409: the client acted on a view of the
 * world that is out of date and should re-read it, which is the meaning 11-contrato-api.md
 * gives to 409.
 */
public class StudentAlreadyManagedException extends RuntimeException {

    public StudentAlreadyManagedException(String message) {
        super(message);
    }
}

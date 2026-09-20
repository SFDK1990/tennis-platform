package com.tennisplatform.student.domain;

/**
 * The teacher tried to stop managing a student who is already deactivated. Maps to 409: the
 * client acted on a view of the world that had already moved on and should re-read it, which
 * is the meaning 11-contrato-api.md gives to 409.
 *
 * <p>Answering "done" instead would be friendlier and wrong: the usual cause is a stale screen
 * or the wrong student, and both are worth knowing about.
 */
public class StudentAlreadyInactiveException extends RuntimeException {

    public StudentAlreadyInactiveException(String message) {
        super(message);
    }
}

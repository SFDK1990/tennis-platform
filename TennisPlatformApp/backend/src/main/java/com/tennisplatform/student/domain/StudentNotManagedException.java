package com.tennisplatform.student.domain;

/**
 * There is no active management of this student by this teacher.
 *
 * <p>Maps to 403 when it answers "you have no relationship with this student", which is the
 * {@code STUDENT_NOT_MANAGED} code of 11-contrato-api.md, and the reason a teacher cannot read
 * the restricted personal data of somebody else's student.
 */
public class StudentNotManagedException extends RuntimeException {

    public StudentNotManagedException(String message) {
        super(message);
    }
}

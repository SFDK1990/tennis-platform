package com.tennisplatform.booking.domain;

/**
 * The student is not currently managed by the teacher of the lesson.
 *
 * <p>Duplicated from {@code student} on purpose, like {@code TeacherRoleRequiredException}
 * elsewhere: the boundary rules let this module see only another module's inbound ports, and
 * a domain exception is not one. Both map to {@code STUDENT_NOT_MANAGED}.
 */
public class StudentNotManagedException extends RuntimeException {

    public StudentNotManagedException(String message) {
        super(message);
    }
}

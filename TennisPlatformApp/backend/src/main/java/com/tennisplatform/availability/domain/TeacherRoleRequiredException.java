package com.tennisplatform.availability.domain;

/**
 * The caller is not the teacher, so they may not change the schedule.
 *
 * <p>This module has its own rather than reusing the one in {@code teacher.domain}: the boundary
 * rules only let availability see teacher's inbound ports, and a shared exception class would
 * be exactly the kind of quiet coupling they exist to prevent. It maps to the same
 * {@code TEACHER_FORBIDDEN} on the wire, which is what the client cares about.
 */
public class TeacherRoleRequiredException extends RuntimeException {

    public TeacherRoleRequiredException(String message) {
        super(message);
    }
}

package com.tennisplatform.student.domain;

/**
 * The caller is not the teacher, and the action is only the teacher's to take. Maps to 403.
 *
 * <p>It lives in this module rather than reusing the teacher module's equivalent because a
 * module may only see another one's inbound ports, never its domain. Two small exception types
 * are the price of a boundary that is actually enforced.
 */
public class TeacherRoleRequiredException extends RuntimeException {

    public TeacherRoleRequiredException(String message) {
        super(message);
    }
}

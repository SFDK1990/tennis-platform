package com.tennisplatform.booking.domain;

/**
 * The caller is not the teacher who owns the lesson or the booking.
 *
 * <p>The fifth copy, and deliberately so: see the same class in {@code lesson} for why a
 * domain exception cannot be shared across modules. Maps to {@code TEACHER_FORBIDDEN}.
 */
public class TeacherRoleRequiredException extends RuntimeException {

    public TeacherRoleRequiredException(String message) {
        super(message);
    }
}

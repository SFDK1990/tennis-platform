package com.tennisplatform.student.domain;

/**
 * The teacher tried to manage a student who has not filled their personal data in yet. Maps to
 * 422: the request is well formed and the student exists, but a business precondition is not
 * met.
 *
 * <p>The schema says the same thing - {@code teacher_students.student_user_id} references
 * {@code student_profiles} - so without this the caller would get a constraint violation
 * instead of an explanation.
 */
public class StudentProfileIncompleteException extends RuntimeException {

    public StudentProfileIncompleteException(String message) {
        super(message);
    }
}

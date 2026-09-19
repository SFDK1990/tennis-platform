package com.tennisplatform.student.domain;

/**
 * No student profile exists for this account yet. Maps to 404.
 *
 * <p>Not an error in the normal flow: a student who has verified their email but has not filled
 * their data in yet is in exactly this state, which is why {@code GET /me} answers with null
 * fields instead of failing. It is raised where a profile is genuinely required.
 */
public class StudentProfileNotFoundException extends RuntimeException {

    public StudentProfileNotFoundException(String message) {
        super(message);
    }
}

package com.tennisplatform.teacher.domain;

/**
 * The caller tried to change a teacher profile that is not theirs. Maps to 403.
 *
 * <p>Having the role is not enough: authorization is by role <em>and</em> by ownership
 * (08-security-engineer.md). In the MVP there is a single teacher, so this can only trigger on
 * a token that claims the role without owning the profile - which is exactly the case worth
 * refusing, and the one that would silently pass if only the role were checked.
 */
public class NotTheTeacherException extends RuntimeException {

    public NotTheTeacherException(String message) {
        super(message);
    }
}

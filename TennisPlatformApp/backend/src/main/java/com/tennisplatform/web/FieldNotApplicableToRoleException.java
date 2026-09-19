package com.tennisplatform.web;

/**
 * The body of {@code PATCH /me} carried a field that belongs to another role. Maps to 400.
 *
 * <p>Refusing rather than quietly dropping it, because a change that is ignored in silence is
 * the one that turns into a bug report: the client believes it saved and it did not. It costs
 * nothing to say which field was wrong, and the field names are not a secret.
 */
class FieldNotApplicableToRoleException extends RuntimeException {

    FieldNotApplicableToRoleException(String message) {
        super(message);
    }
}

package com.tennisplatform.booking.domain;

/**
 * The student has not verified their address yet.
 *
 * <p>01-analisis-funcional.md §9 requires it to book, and nothing earlier enforces it: logging
 * in works without a verified address, and so does filling the profile in and being managed.
 * This is therefore the only place the rule is applied.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}

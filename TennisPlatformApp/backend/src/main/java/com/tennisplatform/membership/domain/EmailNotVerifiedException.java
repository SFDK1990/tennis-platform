package com.tennisplatform.membership.domain;

/** A student joins a group only with a verified address, as when booking a seat. Maps to 403. */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}

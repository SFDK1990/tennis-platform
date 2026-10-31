package com.tennisplatform.membership.domain;

/** A group, a fee or a setting that breaks a rule of its own. Maps to 400. */
public class InvalidMembershipException extends RuntimeException {

    public InvalidMembershipException(String message) {
        super(message);
    }
}

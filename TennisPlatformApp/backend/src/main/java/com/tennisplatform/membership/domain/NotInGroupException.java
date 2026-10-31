package com.tennisplatform.membership.domain;

/** The student is not signed up to the group, or is already leaving it. Maps to 404. */
public class NotInGroupException extends RuntimeException {

    public NotInGroupException(String message) {
        super(message);
    }
}

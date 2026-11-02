package com.tennisplatform.membership.domain;

/** The student is already signed up to the group. Maps to 409. */
public class AlreadyInGroupException extends RuntimeException {

    public AlreadyInGroupException(String message) {
        super(message);
    }
}

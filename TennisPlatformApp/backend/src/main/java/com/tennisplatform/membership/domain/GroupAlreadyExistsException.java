package com.tennisplatform.membership.domain;

/** The teacher already has a group with that name. Maps to 409. */
public class GroupAlreadyExistsException extends RuntimeException {

    public GroupAlreadyExistsException(String message) {
        super(message);
    }
}

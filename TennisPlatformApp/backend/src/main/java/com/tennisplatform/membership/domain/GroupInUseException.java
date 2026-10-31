package com.tennisplatform.membership.domain;

/** Somebody has been signed up to the group, so it cannot be deleted. Maps to 409. */
public class GroupInUseException extends RuntimeException {

    public GroupInUseException(String message) {
        super(message);
    }
}

package com.tennisplatform.membership.domain;

/** No fixed group with that id belongs to the teacher. Maps to 404. */
public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(String message) {
        super(message);
    }
}

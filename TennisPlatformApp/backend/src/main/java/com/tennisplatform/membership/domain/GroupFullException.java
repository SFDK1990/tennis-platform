package com.tennisplatform.membership.domain;

import java.time.Instant;
import java.util.List;

/**
 * Some lesson of the group has no seat left, so nobody new can hold one in every lesson. Maps to
 * 409: the teacher can raise the capacity or free the seat.
 */
public class GroupFullException extends RuntimeException {

    private final transient List<Instant> fullLessons;

    public GroupFullException(String message, List<Instant> fullLessons) {
        super(message);
        this.fullLessons = List.copyOf(fullLessons);
    }

    /** When each full lesson starts. */
    public List<Instant> fullLessons() {
        return fullLessons;
    }
}

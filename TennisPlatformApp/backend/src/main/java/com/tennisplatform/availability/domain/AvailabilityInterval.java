package com.tennisplatform.availability.domain;

import java.time.Instant;

/**
 * A resolved stretch of availability, in real time rather than wall-clock time.
 *
 * <p>This is what leaves the module. Everything outside - lesson validating a class, calendar
 * drawing a week - works in instants, so the ambiguity of a wall clock is resolved here, once,
 * instead of in every caller.
 */
public record AvailabilityInterval(Instant startsAt, Instant endsAt) {

    public AvailabilityInterval {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new InvalidAvailabilityException("An interval must start before it ends");
        }
    }

    /** Half-open, like the wall-clock ranges it comes from: an interval covers its own bounds. */
    public boolean covers(Instant from, Instant to) {
        return !from.isBefore(startsAt) && !to.isAfter(endsAt);
    }

    boolean meetsOrOverlaps(AvailabilityInterval other) {
        return !other.startsAt.isAfter(endsAt) && !startsAt.isAfter(other.endsAt);
    }
}

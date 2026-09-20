package com.tennisplatform.availability.application.port.in;

import com.tennisplatform.availability.domain.AvailabilityInterval;

import java.time.Instant;

/** A resolved stretch of availability, in instants, for the modules that consume it. */
public record AvailabilityIntervalView(Instant startsAt, Instant endsAt) {

    public static AvailabilityIntervalView from(AvailabilityInterval interval) {
        return new AvailabilityIntervalView(interval.startsAt(), interval.endsAt());
    }
}

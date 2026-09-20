package com.tennisplatform.availability.application.port.in;

import com.tennisplatform.availability.domain.AvailabilityOverride;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A date exception as anything outside this module sees it. Null times on a {@code BLOCK} mean
 * the whole day.
 */
public record AvailabilityOverrideView(UUID id, LocalDate date, LocalTime startTime, LocalTime endTime,
                                String type) {

    public static AvailabilityOverrideView from(AvailabilityOverride exception) {
        return new AvailabilityOverrideView(exception.id(), exception.date(), exception.startTime(),
                exception.endTime(), exception.type().name());
    }
}

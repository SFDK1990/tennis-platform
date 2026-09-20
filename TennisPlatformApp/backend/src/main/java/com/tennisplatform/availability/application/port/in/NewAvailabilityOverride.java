package com.tennisplatform.availability.application.port.in;

import java.time.LocalDate;
import java.time.LocalTime;

/** A block or an extra to add to one date. Null times on a block mean the whole day. */
public record NewAvailabilityOverride(LocalDate date, LocalTime startTime, LocalTime endTime, String type) {
}

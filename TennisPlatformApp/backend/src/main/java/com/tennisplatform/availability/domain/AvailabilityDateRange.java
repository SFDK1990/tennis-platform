package com.tennisplatform.availability.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A bounded, inclusive range of dates.
 *
 * <p>The cap exists because exceptions accumulate for ever: without it, reading the availability
 * is a query whose cost grows every season, and "current exceptions" is a phrase with no
 * checkable meaning. 62 days is the number 11-contrato-api.md suggested for {@code /calendar};
 * it is fixed here, and calendar will inherit it rather than pick a second one.
 */
public record AvailabilityDateRange(LocalDate from, LocalDate to) {

    public static final int MAX_DAYS = 62;

    public AvailabilityDateRange {
        if (from == null || to == null) {
            throw new InvalidAvailabilityException("Both 'from' and 'to' are required");
        }
        if (to.isBefore(from)) {
            throw new InvalidAvailabilityException("'to' cannot be before 'from'");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_DAYS) {
            throw new AvailabilityRangeTooWideException(
                    "The range cannot be wider than " + MAX_DAYS + " days, and this one is " + days);
        }
    }
}

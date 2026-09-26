package com.tennisplatform.shared.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A bounded, inclusive range of dates, as every "from/to" query of the API takes it.
 *
 * <p>The cap exists because what is read accumulates for ever, and an unbounded query gets slower
 * every season. One number for every such query: two different caps would be a difference nobody
 * decided.
 */
public record DateRange(LocalDate from, LocalDate to) {

    public static final int MAX_DAYS = 62;

    public DateRange {
        if (from == null || to == null) {
            throw new InvalidDateRangeException("Both 'from' and 'to' are required");
        }
        if (to.isBefore(from)) {
            throw new InvalidDateRangeException("'to' cannot be before 'from'");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_DAYS) {
            throw new InvalidDateRangeException(
                    "The range cannot be wider than " + MAX_DAYS + " days, and this one is " + days);
        }
    }

    public static class InvalidDateRangeException extends RuntimeException {

        public InvalidDateRangeException(String message) {
            super(message);
        }
    }
}

package com.tennisplatform.lesson.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A bounded, inclusive range of dates for listing lessons.
 *
 * <p>Same shape and same cap as {@code AvailabilityDateRange}, and the duplication is forced
 * rather than careless: the boundary rules let a module see another module's
 * {@code application/port/in} and nothing else, so a domain type cannot be shared. What can be
 * shared is the number, and it deliberately is - a teacher who can read two months of
 * availability and only one of lessons would have found a difference nobody decided.
 *
 * <p>The cap exists because lessons accumulate for ever: without it, listing them is a query
 * whose cost grows every season.
 */
public record LessonDateRange(LocalDate from, LocalDate to) {

    public static final int MAX_DAYS = 62;

    public LessonDateRange {
        if (from == null || to == null) {
            throw new InvalidLessonException("Both 'from' and 'to' are required");
        }
        if (to.isBefore(from)) {
            throw new InvalidLessonException("'to' cannot be before 'from'");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_DAYS) {
            throw new LessonRangeTooWideException(
                    "The range cannot be wider than " + MAX_DAYS + " days, and this one is " + days);
        }
    }
}

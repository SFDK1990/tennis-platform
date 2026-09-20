package com.tennisplatform.availability.application.port.in;

import com.tennisplatform.availability.domain.WeeklyAvailabilityRule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A weekly rule as anything outside this module sees it.
 *
 * <p>The weekday travels as its name, not as a number. Three numbering conventions meet around
 * this field - the API spoke of 0 = Monday, {@code java.time} numbers Monday 1 and PostgreSQL's
 * {@code EXTRACT(DOW)} numbers Sunday 0 - and a wrong number shifts the whole schedule by a day
 * without failing anywhere. A wrong name cannot be parsed.
 *
 * <p>{@code activeFrom} and {@code activeUntil} are here because the weekly configuration is
 * replaced wholesale: whatever this view omits, a client that reads, edits and writes back would
 * silently delete.
 */
public record WeeklyRuleView(UUID id, String dayOfWeek, LocalTime startTime, LocalTime endTime,
                             LocalDate activeFrom, LocalDate activeUntil) {

    public static WeeklyRuleView from(WeeklyAvailabilityRule rule) {
        return new WeeklyRuleView(rule.id(), rule.dayOfWeek().name(), rule.startTime(),
                rule.endTime(), rule.activeFrom(), rule.activeUntil());
    }
}

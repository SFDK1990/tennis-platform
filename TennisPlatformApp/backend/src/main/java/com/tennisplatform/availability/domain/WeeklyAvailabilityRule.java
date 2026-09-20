package com.tennisplatform.availability.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * "Every Monday from 09:00 to 13:00", optionally only between two dates.
 *
 * <p>The times are wall-clock in the teacher's own zone, not instants. How long the rule lasts
 * in real time, and which instants it maps to, depends on the date - see
 * {@link AvailabilitySchedule}.
 *
 * <p>The weekday is a {@link DayOfWeek} rather than a number, which settles a collision that
 * would otherwise be invisible: the API spoke of 0 = Monday, {@code java.time} numbers Monday 1,
 * and PostgreSQL's {@code EXTRACT(DOW)} numbers Sunday 0. A mistake between the three does not
 * fail - it silently moves the schedule by a day. The type is the fix; nothing in this module
 * ever handles the number.
 */
public class WeeklyAvailabilityRule {

    private final UUID id;
    private final UUID teacherUserId;
    private final DayOfWeek dayOfWeek;
    private final LocalTimeRange hours;
    private final LocalDate activeFrom;
    private final LocalDate activeUntil;

    private WeeklyAvailabilityRule(UUID id, UUID teacherUserId, DayOfWeek dayOfWeek,
                                   LocalTimeRange hours, LocalDate activeFrom, LocalDate activeUntil) {
        this.id = id;
        this.teacherUserId = teacherUserId;
        this.dayOfWeek = dayOfWeek;
        this.hours = hours;
        this.activeFrom = activeFrom;
        this.activeUntil = activeUntil;
    }

    public static WeeklyAvailabilityRule create(UUID teacherUserId, DayOfWeek dayOfWeek,
                                                LocalTime startTime, LocalTime endTime,
                                                LocalDate activeFrom, LocalDate activeUntil) {
        if (teacherUserId == null) {
            throw new InvalidAvailabilityException("A rule must belong to a teacher");
        }
        if (dayOfWeek == null) {
            throw new InvalidAvailabilityException("A rule must name a day of the week");
        }
        if (activeFrom != null && activeUntil != null && activeUntil.isBefore(activeFrom)) {
            throw new InvalidAvailabilityException(
                    "The rule cannot stop being active before it starts: " + activeFrom + " to " + activeUntil);
        }
        return new WeeklyAvailabilityRule(null, teacherUserId, dayOfWeek,
                new LocalTimeRange(startTime, endTime), activeFrom, activeUntil);
    }

    /**
     * Parses the weekday as its ISO name, rejecting anything else.
     *
     * <p>Accepting a number here would reopen the collision the type exists to close: 0, 1 and 7
     * all mean something to somebody, and picking one silently shifts the schedule.
     */
    public static java.time.DayOfWeek parseDayOfWeek(String value) {
        if (value == null) {
            throw new InvalidAvailabilityException("A rule must name a day of the week");
        }
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidAvailabilityException(
                    "Unknown day of the week: " + value + ". Expected MONDAY to SUNDAY");
        }
    }

    /** Rehydration from persistence. Stored values are not re-validated. */
    public static WeeklyAvailabilityRule rehydrate(UUID id, UUID teacherUserId, DayOfWeek dayOfWeek,
                                                   LocalTime startTime, LocalTime endTime,
                                                   LocalDate activeFrom, LocalDate activeUntil) {
        return new WeeklyAvailabilityRule(id, teacherUserId, dayOfWeek,
                new LocalTimeRange(startTime, endTime), activeFrom, activeUntil);
    }

    /** Whether this rule has anything to say about a given date. */
    public boolean appliesOn(LocalDate date) {
        return dayOfWeek == date.getDayOfWeek()
                && (activeFrom == null || !date.isBefore(activeFrom))
                && (activeUntil == null || !date.isAfter(activeUntil));
    }

    /**
     * Whether two rules of the same weekday would ever be in force at the same time.
     *
     * <p>Both halves have to overlap to be a conflict: the same hours on the same weekday are
     * fine if one rule stops being active before the other starts, which is exactly how a
     * schedule change is expressed.
     */
    public boolean conflictsWith(WeeklyAvailabilityRule other) {
        return dayOfWeek == other.dayOfWeek
                && hours.overlaps(other.hours)
                && activePeriodsOverlap(other);
    }

    private boolean activePeriodsOverlap(WeeklyAvailabilityRule other) {
        boolean endsBeforeOtherStarts = activeUntil != null && other.activeFrom != null
                && activeUntil.isBefore(other.activeFrom);
        boolean startsAfterOtherEnds = activeFrom != null && other.activeUntil != null
                && other.activeUntil.isBefore(activeFrom);
        return !endsBeforeOtherStarts && !startsAfterOtherEnds;
    }

    public UUID id() {
        return id;
    }

    public UUID teacherUserId() {
        return teacherUserId;
    }

    public DayOfWeek dayOfWeek() {
        return dayOfWeek;
    }

    public LocalTimeRange hours() {
        return hours;
    }

    public LocalTime startTime() {
        return hours.start();
    }

    public LocalTime endTime() {
        return hours.end();
    }

    public LocalDate activeFrom() {
        return activeFrom;
    }

    public LocalDate activeUntil() {
        return activeUntil;
    }
}

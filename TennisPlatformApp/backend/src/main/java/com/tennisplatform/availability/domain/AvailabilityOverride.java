package com.tennisplatform.availability.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

/**
 * A one-off change to a single date: time removed ({@code BLOCK}) or time added ({@code EXTRA}).
 *
 * <p>Named {@code AvailabilityOverride} rather than {@code AvailabilityException} on
 * purpose. The obvious name reads as a Java exception everywhere it appears - in a catch block,
 * in a stack trace, in an import list next to the three real exceptions of this package - and
 * this class is a piece of the schedule, not a failure.
 *
 * <p>A {@code BLOCK} with no hours means the whole day. An {@code EXTRA} must carry both, since
 * "extra availability, some time that day" says nothing a calendar can draw. That rule lives
 * here instead of in the schema, as 10-diagrama-er.md decided, so it is not encoded as a
 * combination of NULLs that only a comment explains.
 */
public class AvailabilityOverride {

    private final UUID id;
    private final UUID teacherUserId;
    private final LocalDate date;
    private final LocalTimeRange hours;
    private final AvailabilityOverrideType type;

    private AvailabilityOverride(UUID id, UUID teacherUserId, LocalDate date,
                                      LocalTimeRange hours, AvailabilityOverrideType type) {
        this.id = id;
        this.teacherUserId = teacherUserId;
        this.date = date;
        this.hours = hours;
        this.type = type;
    }

    public static AvailabilityOverride create(UUID teacherUserId, LocalDate date,
                                                   LocalTime startTime, LocalTime endTime,
                                                   AvailabilityOverrideType type) {
        if (teacherUserId == null) {
            throw new InvalidAvailabilityException("An exception must belong to a teacher");
        }
        if (date == null) {
            throw new InvalidAvailabilityException("An exception must name a date");
        }
        if (type == null) {
            throw new InvalidAvailabilityException("An exception must be either BLOCK or EXTRA");
        }
        return new AvailabilityOverride(null, teacherUserId, date,
                hoursFor(type, startTime, endTime), type);
    }

    /** Rehydration from persistence. Stored values are not re-validated. */
    public static AvailabilityOverride rehydrate(UUID id, UUID teacherUserId, LocalDate date,
                                                      LocalTime startTime, LocalTime endTime,
                                                      AvailabilityOverrideType type) {
        LocalTimeRange hours = startTime == null && endTime == null
                ? null
                : new LocalTimeRange(startTime, endTime);
        return new AvailabilityOverride(id, teacherUserId, date, hours, type);
    }

    private static LocalTimeRange hoursFor(AvailabilityOverrideType type, LocalTime start, LocalTime end) {
        if (type == AvailabilityOverrideType.EXTRA) {
            if (start == null || end == null) {
                throw new InvalidAvailabilityException(
                        "Extra availability needs a start and an end time");
            }
            return new LocalTimeRange(start, end);
        }
        if (start == null && end == null) {
            return null;
        }
        if (start == null || end == null) {
            throw new InvalidAvailabilityException(
                    "A block needs both times or neither: one alone does not describe anything");
        }
        return new LocalTimeRange(start, end);
    }

    /** A block with no hours: the entire date is unavailable, whatever the weekly rules say. */
    public boolean blocksTheWholeDay() {
        return type == AvailabilityOverrideType.BLOCK && hours == null;
    }

    public Optional<LocalTimeRange> hours() {
        return Optional.ofNullable(hours);
    }

    public UUID id() {
        return id;
    }

    public UUID teacherUserId() {
        return teacherUserId;
    }

    public LocalDate date() {
        return date;
    }

    public AvailabilityOverrideType type() {
        return type;
    }

    public LocalTime startTime() {
        return hours == null ? null : hours.start();
    }

    public LocalTime endTime() {
        return hours == null ? null : hours.end();
    }
}

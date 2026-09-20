package com.tennisplatform.availability.domain;

/**
 * What a date exception does to the weekly schedule.
 *
 * <p>{@code BLOCK} removes time, {@code EXTRA} adds it. When both apply to the same date the
 * block wins, and the reason is asymmetry of harm: misreading a block puts the teacher in front
 * of a student on a day they said they could not, while misreading an extra only loses a slot
 * that can be added again.
 */
public enum AvailabilityOverrideType {
    BLOCK,
    EXTRA;

    /** Parses the wire value, rejecting anything else rather than defaulting to one of the two. */
    public static AvailabilityOverrideType parse(String value) {
        if (value == null) {
            throw new InvalidAvailabilityException("An exception must be either BLOCK or EXTRA");
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidAvailabilityException(
                    "Unknown exception type: " + value + ". Expected BLOCK or EXTRA");
        }
    }
}

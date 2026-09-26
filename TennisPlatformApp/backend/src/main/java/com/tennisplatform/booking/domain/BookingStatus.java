package com.tennisplatform.booking.domain;

import java.util.Locale;

/**
 * Whether a booking still stands, and if not, who ended it.
 *
 * <p>Who cancelled is part of the status rather than a separate column because it is exactly
 * what the student sees - "cancelled by the teacher" and "cancelled by you" are different
 * messages. Attendance is <em>not</em> here; see {@link Attendance}.
 */
public enum BookingStatus {
    CONFIRMED,
    CANCELLED_BY_STUDENT,
    CANCELLED_BY_TEACHER,
    CANCELLED_BY_ADMIN;

    /**
     * An optional filter from a query string: absent means "any status". Parsed here rather than
     * by Spring's converter, whose failure on an unknown value escapes every handler and comes
     * back as a 500.
     */
    public static BookingStatus filter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalised = value.trim().toUpperCase(Locale.ROOT);
        for (BookingStatus status : values()) {
            if (status.name().equals(normalised)) {
                return status;
            }
        }
        throw new InvalidBookingRequestException("Unknown booking status: " + value);
    }
}

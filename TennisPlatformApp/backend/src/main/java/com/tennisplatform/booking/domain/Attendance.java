package com.tennisplatform.booking.domain;

import java.util.Locale;

/**
 * Whether the student came to the lesson. Separate from {@link BookingStatus}: a booking that
 * stands and a student who showed up are two different facts.
 */
public enum Attendance {
    PENDING,
    ATTENDED,
    NO_SHOW;

    /**
     * What a teacher may mark. {@code PENDING} is where every booking starts and not something
     * to go back to: once the lesson has started, the student either came or did not.
     */
    public static Attendance markable(String value) {
        if (value != null) {
            String normalised = value.trim().toUpperCase(Locale.ROOT);
            if (ATTENDED.name().equals(normalised)) {
                return ATTENDED;
            }
            if (NO_SHOW.name().equals(normalised)) {
                return NO_SHOW;
            }
        }
        throw new InvalidBookingRequestException("Attendance is marked as ATTENDED or NO_SHOW, not " + value);
    }
}

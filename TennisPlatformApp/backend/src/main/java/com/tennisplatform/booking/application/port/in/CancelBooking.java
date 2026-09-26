package com.tennisplatform.booking.application.port.in;

import java.util.UUID;

/**
 * Cancelling a single booking. Three entry points rather than one with a role parameter, because
 * the three differ in exactly what matters - which bookings the caller may touch and whether the
 * 24-hour window binds them - and a role flag would hide that inside a branch.
 */
public interface CancelBooking {

    /** Their own booking only, with at least 24 hours of notice. */
    BookingView asStudent(UUID studentUserId, UUID bookingId);

    /** Any booking in one of their lessons, at any time before it starts. */
    BookingView asTeacher(UUID teacherUserId, UUID bookingId);

    /** Any booking, at any time before its lesson starts: the way out for incidents. */
    BookingView asAdmin(UUID bookingId);
}

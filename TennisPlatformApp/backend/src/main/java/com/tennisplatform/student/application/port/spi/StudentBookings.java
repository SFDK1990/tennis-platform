package com.tennisplatform.student.application.port.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * What this module needs done to a student's bookings, which only {@code booking} can do.
 *
 * <p>Runs inside the caller's transaction, so a deactivation whose bookings cannot be cancelled
 * does not happen either: the student is never left inactive while still holding seats.
 */
public interface StudentBookings {

    /**
     * Cancels the student's confirmed bookings in this teacher's lessons that have not started.
     *
     * <p>Only this teacher's: with a single teacher it is the same as "all of them", and the day
     * there are two, being let go by one must not empty the student's diary with the other.
     * Bookings of lessons that already started are left alone - those lessons happened, and
     * whatever attendance they recorded is history worth keeping.
     */
    void cancelUpcomingWith(UUID teacherUserId, UUID studentUserId, Instant at);

    /**
     * The same, when it was an administrator who disabled the account. A separate method
     * because the cancelled bookings must say who decided: a student who finds their seat gone
     * should know whether it was their teacher or the administration.
     */
    void cancelUpcomingOfDisabledAccount(UUID teacherUserId, UUID studentUserId, Instant at);
}

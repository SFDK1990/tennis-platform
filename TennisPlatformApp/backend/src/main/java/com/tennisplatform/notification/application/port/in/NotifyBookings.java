package com.tennisplatform.notification.application.port.in;

import com.tennisplatform.booking.application.port.in.BookingCancelled;
import com.tennisplatform.booking.application.port.in.BookingConfirmed;

/**
 * Tells the people a booking concerns what happened to it, by email (Fase 20, 01 section 18).
 *
 * <p>Called after the booking's transaction has committed. It never throws: an email that cannot
 * be written or sent is logged and counted, and the booking stands.
 */
public interface NotifyBookings {

    /** The student gets the confirmation with the lesson for their calendar; the teacher, a notice. */
    void confirmed(BookingConfirmed booking);

    /**
     * Who hears depends only on who cancelled: a student's cancellation goes to the teacher, the
     * teacher's or an administrator's to the student, with a calendar cancellation.
     */
    void cancelled(BookingCancelled booking);
}

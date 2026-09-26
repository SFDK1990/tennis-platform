package com.tennisplatform.booking.application.port.in;

import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.lesson.application.port.in.LessonView;

import java.time.Instant;
import java.util.UUID;

/**
 * A booking as anybody outside this module sees it, with its lesson attached: every screen that
 * shows a booking also shows when it is, and asking for the lesson one booking at a time is the
 * N+1 that the embedded copy avoids.
 *
 * <p>Status and attendance travel as strings, for the same reason {@link LessonView} does it:
 * a caller that received this module's enums would depend on its domain.
 */
public record BookingView(UUID id, UUID lessonId, UUID studentUserId, String status,
                          String attendance, Instant bookedAt, Instant cancelledAt,
                          LessonView lesson) {

    public static BookingView from(Booking booking, LessonView lesson) {
        return new BookingView(booking.id(), booking.lessonId(), booking.studentUserId(),
                booking.status().name(), booking.attendance().name(), booking.bookedAt(),
                booking.cancelledAt(), lesson);
    }
}

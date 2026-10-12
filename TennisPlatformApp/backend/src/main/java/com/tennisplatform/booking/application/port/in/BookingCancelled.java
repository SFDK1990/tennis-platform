package com.tennisplatform.booking.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * A booking has just been cancelled, one by one or with its whole lesson. Heard after the commit,
 * like {@link BookingConfirmed}.
 *
 * <p>{@code status} says who cancelled, as a wire value ({@code CANCELLED_BY_STUDENT},
 * {@code CANCELLED_BY_TEACHER}, {@code CANCELLED_BY_ADMIN}). {@code reason} is what the teacher
 * wrote when cancelling the lesson or the day, or null: it travels to the student's email and is
 * never stored.
 *
 * <p>Not published when a student deletes their account: the address is gone by then, and the
 * teacher learns that the student left from the student list.
 */
public record BookingCancelled(UUID bookingId, UUID lessonId, UUID teacherUserId, UUID studentUserId,
                               Instant startsAt, Instant endsAt, String status, String reason) {
}

package com.tennisplatform.booking.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A student waiting for a seat in a full lesson (34-fase21-analisis-lista-espera.md).
 *
 * <p>The list closes {@link #CLOSES_BEFORE_START} before the lesson: from then on a seat that
 * frees up is handed to nobody, because a booking made that late can no longer be cancelled, and
 * the list would be booking a student into a lesson they cannot get out of.
 */
public record WaitingListEntry(UUID id, UUID lessonId, UUID teacherUserId, UUID studentUserId, Instant joinedAt) {

    public static final Duration CLOSES_BEFORE_START = Booking.CANCELLATION_WINDOW;

    public static WaitingListEntry join(UUID lessonId, UUID teacherUserId, UUID studentUserId, Instant now) {
        return new WaitingListEntry(null, lessonId, teacherUserId, studentUserId, now);
    }

    /** Exactly 24 hours before is still open, as with cancelling: a limit keeps its own boundary. */
    public static boolean isOpen(Instant lessonStartsAt, Instant now) {
        return Duration.between(now, lessonStartsAt).compareTo(CLOSES_BEFORE_START) >= 0;
    }
}

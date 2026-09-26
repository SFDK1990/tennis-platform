package com.tennisplatform.lesson.application.port.spi;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * What this module needs to know and do about the bookings of its lessons, which only
 * {@code booking} can.
 *
 * <p>Both operations run inside the caller's transaction. That is what makes the cascade on
 * cancellation all-or-nothing - if it fails, the lesson stays open - and what lets a count made
 * after locking a lesson see every booking that got in before the lock.
 */
public interface LessonBookings {

    /**
     * The confirmed bookings of each lesson. A lesson with none is simply absent from the map,
     * so a batch of a thousand lessons costs one query and not a thousand.
     */
    Map<UUID, Integer> countConfirmed(Collection<UUID> lessonIds);

    /** Cancels every confirmed booking of a lesson the teacher has just cancelled. */
    void cancelAllOf(UUID lessonId, Instant cancelledAt);
}

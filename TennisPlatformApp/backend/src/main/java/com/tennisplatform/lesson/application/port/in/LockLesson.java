package com.tennisplatform.lesson.application.port.in;

import java.util.UUID;

/**
 * Reads a lesson while holding a lock on it, so that nobody else can take a seat until the
 * caller's transaction ends.
 *
 * <p>This is how the last seat is protected. Capacity cannot be a schema constraint, because
 * checking it means counting rows of another table; instead every booking locks its lesson,
 * counts and inserts, and two bookings of the same lesson queue up at the lock. The count in the
 * returned view is taken <em>after</em> the lock is held, which is what makes it trustworthy.
 *
 * <p>It refuses to run outside a transaction: a lock that is released when this method returns
 * protects nothing, and failing loudly is better than a race that passes every test run alone.
 */
public interface LockLesson {

    LessonView lockForBooking(UUID lessonId);
}

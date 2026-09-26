package com.tennisplatform.lesson.application.port.out;

import com.tennisplatform.lesson.domain.Lesson;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository {

    /**
     * Saves a new lesson or an updated one.
     *
     * <p>Throws {@code LessonOverlapException} when the exclusion constraint rejects it. The
     * translation happens in the adapter, which is the only layer that knows what a constraint
     * violation looks like - and it has to happen, because between the application's own check
     * and this write another request fits.
     */
    Lesson save(Lesson lesson);

    Optional<Lesson> findById(UUID id);

    /** Reads the lesson with {@code SELECT ... FOR UPDATE}; the lock lasts until the transaction ends. */
    Optional<Lesson> findByIdForUpdate(UUID id);

    List<Lesson> findAllById(Collection<UUID> ids);

    /** The teacher's lessons that touch the interval, cancelled ones included, earliest first. */
    List<Lesson> findByTeacherBetween(UUID teacherUserId, Instant from, Instant to);

    /** Whether a non-cancelled lesson of this teacher already occupies any part of the interval. */
    boolean existsOverlapping(UUID teacherUserId, Instant startsAt, Instant endsAt);
}

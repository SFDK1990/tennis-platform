package com.tennisplatform.lesson.adapters.out.persistence;

import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonOverlapException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
class LessonRepositoryAdapter implements LessonRepository {

    /**
     * The exclusion constraint's own name, as the v6-lesson changeset declares it. Matching on
     * the name rather than on the message text is what keeps this from turning every integrity
     * violation into an overlap: a lesson can also break the duration CHECK or the foreign key,
     * and those are not the same answer at all.
     */
    private static final String OVERLAP_CONSTRAINT = "ex_lessons_no_teacher_overlap";

    private final LessonJpaRepository jpa;

    LessonRepositoryAdapter(LessonJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * Writes the lesson, translating the exclusion constraint into the same answer the
     * application's own check gives.
     *
     * <p>{@code saveAndFlush} rather than {@code save} on purpose: without the flush the
     * violation surfaces when the transaction commits, which is after this method - and after
     * the controller - has returned, so it would arrive as a 500 with no way to map it.
     */
    @Override
    public Lesson save(Lesson lesson) {
        try {
            return jpa.saveAndFlush(entityFor(lesson)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (violatesOverlap(e)) {
                throw new LessonOverlapException("Another lesson already runs at that time");
            }
            throw e;
        }
    }

    /**
     * An existing lesson is loaded and updated rather than overwritten by a detached copy, so
     * the columns this module does not map - {@code version}, {@code created_at} - keep the
     * values the database gave them.
     */
    private LessonEntity entityFor(Lesson lesson) {
        if (lesson.id() == null) {
            return LessonEntity.fromDomain(lesson);
        }
        return jpa.findById(lesson.id())
                .map(existing -> {
                    existing.apply(lesson);
                    return existing;
                })
                .orElseGet(() -> LessonEntity.fromDomain(lesson));
    }

    private boolean violatesOverlap(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(OVERLAP_CONSTRAINT);
    }

    @Override
    public Optional<Lesson> findById(UUID id) {
        return jpa.findById(id).map(LessonEntity::toDomain);
    }

    @Override
    public List<Lesson> findByTeacherBetween(UUID teacherUserId, Instant from, Instant to) {
        return jpa.findTouching(teacherUserId, from, to).stream().map(LessonEntity::toDomain).toList();
    }

    @Override
    public boolean existsOverlapping(UUID teacherUserId, Instant startsAt, Instant endsAt) {
        return jpa.existsOverlapping(teacherUserId, startsAt, endsAt);
    }
}

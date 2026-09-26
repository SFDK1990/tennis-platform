package com.tennisplatform.lesson.adapters.out.persistence;

import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lessons")
class LessonEntity {

    /** What the column holds: only the two states a person decides. The rest is derived on read. */
    private static final String OPEN = "OPEN";
    private static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "teacher_user_id", nullable = false)
    private UUID teacherUserId;

    /** STRING, not ORDINAL: the column has a CHECK on the literals and reordering the enum must not silently remap existing rows. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private LessonType type;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "capacity", nullable = false)
    private Short capacity;

    /**
     * Kept in step with {@link #cancelledAt} by {@link #apply}, and the schema checks it:
     * {@code (status = 'CANCELLED') = (cancelled_at IS NOT NULL)}. It exists as its own column
     * because the exclusion constraint has to filter on it, and a partial index cannot be built
     * on "is this other column null" as readably.
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_outside_availability", nullable = false)
    private boolean createdOutsideAvailability;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // The "version" column exists in the schema and is deliberately not mapped yet. Fase 9 adds
    // the optimistic locking that gives it meaning, together with the SELECT ... FOR UPDATE that
    // protects the last seat; mapping it now would only mean carrying a number nothing reads.

    protected LessonEntity() {
    }

    static LessonEntity fromDomain(Lesson lesson) {
        LessonEntity entity = new LessonEntity();
        entity.id = lesson.id();
        entity.teacherUserId = lesson.teacherUserId();
        entity.apply(lesson);
        return entity;
    }

    /** Copies the mutable half onto an entity that may already be managed by the persistence context. */
    void apply(Lesson lesson) {
        this.type = lesson.type();
        this.startsAt = lesson.startsAt();
        this.endsAt = lesson.endsAt();
        this.capacity = (short) lesson.capacity();
        this.status = lesson.isCancelled() ? CANCELLED : OPEN;
        this.notes = lesson.notes();
        this.createdOutsideAvailability = lesson.createdOutsideAvailability();
        this.cancelledAt = lesson.cancelledAt();
    }

    Lesson toDomain() {
        return Lesson.rehydrate(id, teacherUserId, type, startsAt, endsAt, capacity, notes,
                createdOutsideAvailability, cancelledAt);
    }
}

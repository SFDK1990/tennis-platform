package com.tennisplatform.lesson.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * A lesson the teacher gives, individual or group.
 *
 * <p>What is stored is only what a person decided: when it is, for how many, and whether it was
 * cancelled. Everything that follows from those - whether it has finished, whether it was
 * cancelled at short notice - is worked out here rather than written down, so there is no second
 * copy to fall out of step. See {@link LessonStatus}.
 *
 * <p>Immutable: cancelling returns a new lesson rather than changing this one, which keeps the
 * rule "a cancelled lesson cannot be reopened" a property of the type instead of a check
 * somebody has to remember.
 */
public class Lesson {

    /**
     * How much notice makes a cancellation a normal one.
     *
     * <p>It does not forbid anything here. The teacher may cancel their own lesson at any time -
     * a teacher who falls ill the night before has to be able to - but whether it happened
     * inside this window is recorded, so the students affected can be told. The window that
     * does forbid is the student's, over their own booking, and that one lives in {@code
     * booking}.
     */
    public static final Duration CANCELLATION_NOTICE = Duration.ofHours(24);

    private final UUID id;
    private final UUID teacherUserId;
    private final LessonType type;
    private final LessonPeriod period;
    private final int capacity;
    private final String notes;
    private final boolean createdOutsideAvailability;
    private final Instant cancelledAt;

    private Lesson(UUID id, UUID teacherUserId, LessonType type, LessonPeriod period, int capacity,
                   String notes, boolean createdOutsideAvailability, Instant cancelledAt) {
        this.id = id;
        this.teacherUserId = teacherUserId;
        this.type = type;
        this.period = period;
        this.capacity = capacity;
        this.notes = notes;
        this.createdOutsideAvailability = createdOutsideAvailability;
        this.cancelledAt = cancelledAt;
    }

    /**
     * A new lesson, validated.
     *
     * <p>{@code teacherZone} is needed only to decide whether the lesson crosses midnight, which
     * is a local question. {@code maxGroupCapacity} comes from the platform configuration rather
     * than being a constant here: the day the number is wrong it is wrong for every lesson at
     * once, and changing it should not need a deploy.
     */
    public static Lesson create(UUID teacherUserId, LessonType type, LessonPeriod period, int capacity,
                                String notes, boolean createdOutsideAvailability,
                                ZoneId teacherZone, int maxGroupCapacity) {
        if (teacherUserId == null) {
            throw new InvalidLessonException("A lesson must belong to a teacher");
        }
        if (type == null) {
            throw new InvalidLessonException("A lesson must be either INDIVIDUAL or GROUP");
        }
        if (period == null) {
            throw new InvalidLessonException("A lesson needs a start and an end");
        }
        if (teacherZone == null) {
            throw new InvalidLessonException("The teacher's time zone is needed to place the lesson in a day");
        }
        if (period.crossesMidnightIn(teacherZone)) {
            throw new InvalidLessonException("A lesson cannot run past midnight in the teacher's zone");
        }
        requireCapacityFor(type, capacity, maxGroupCapacity);

        return new Lesson(null, teacherUserId, type, period, capacity, normalise(notes),
                createdOutsideAvailability, null);
    }

    /** Rehydration from persistence. Stored values are not re-validated. */
    public static Lesson rehydrate(UUID id, UUID teacherUserId, LessonType type, Instant startsAt,
                                   Instant endsAt, int capacity, String notes,
                                   boolean createdOutsideAvailability, Instant cancelledAt) {
        return new Lesson(id, teacherUserId, type, new LessonPeriod(startsAt, endsAt), capacity, notes,
                createdOutsideAvailability, cancelledAt);
    }

    private static void requireCapacityFor(LessonType type, int capacity, int maxGroupCapacity) {
        if (capacity < 1) {
            throw new InvalidLessonException("A lesson holds at least one student, not " + capacity);
        }
        if (type == LessonType.INDIVIDUAL && capacity != 1) {
            throw new InvalidLessonException(
                    "An individual lesson holds exactly one student, not " + capacity);
        }
        if (type == LessonType.GROUP && capacity > maxGroupCapacity) {
            throw new InvalidLessonException(
                    "A group lesson holds at most " + maxGroupCapacity + " students, not " + capacity);
        }
    }

    /** Blank notes are stored as nothing at all, so "empty" has one representation instead of two. */
    private static String normalise(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.trim();
    }

    /**
     * Cancels the lesson, which frees its slot for another one.
     *
     * <p>A cancelled lesson is never reopened: its bookings are cancelled with it, and bringing
     * it back would leave those students out without telling them. If the teacher changes their
     * mind, they create another lesson - the slot is free again precisely because this one is
     * cancelled.
     */
    public Lesson cancel(Instant now) {
        if (cancelledAt != null) {
            throw new LessonAlreadyCancelledException("The lesson was already cancelled");
        }
        if (!period.endsAt().isAfter(now)) {
            throw new LessonAlreadyFinishedException("The lesson already ended and cannot be cancelled");
        }
        return new Lesson(id, teacherUserId, type, period, capacity, notes, createdOutsideAvailability, now);
    }

    /**
     * Changes the notes and the capacity, the only two things that may change
     * (30-fase19-analisis-cierre-mvp.md): the time cannot, because the students booked that time.
     * A null leaves the value as it is; blank notes clear them.
     *
     * <p>The count must come from a read made under the lesson's lock, or a booking that got in
     * meanwhile would be left without a seat.
     */
    public Lesson edit(String newNotes, Integer newCapacity, int confirmedBookings, Instant now,
                       int maxGroupCapacity) {
        if (cancelledAt != null) {
            throw new LessonAlreadyCancelledException("A cancelled lesson cannot be changed");
        }
        if (!period.startsAt().isAfter(now)) {
            throw new LessonAlreadyStartedException("The lesson has started and can no longer be changed");
        }
        int capacityAfter = newCapacity == null ? capacity : newCapacity;
        if (newCapacity != null) {
            requireCapacityFor(type, capacityAfter, maxGroupCapacity);
            if (capacityAfter < confirmedBookings) {
                throw new LessonCapacityBelowBookingsException("The lesson already has " + confirmedBookings
                        + " students booked, more than " + capacityAfter + " seats");
            }
        }
        String notesAfter = newNotes == null ? notes : normalise(newNotes);
        return new Lesson(id, teacherUserId, type, period, capacityAfter, notesAfter, createdOutsideAvailability,
                cancelledAt);
    }

    /**
     * What the lesson looks like right now, given how many confirmed bookings it has.
     *
     * <p>The order matters and is deliberate: a lesson that was cancelled reads {@code
     * CANCELLED} whatever else is true of it, and a full lesson that has already finished reads
     * {@code COMPLETED}, because what anybody wants to know about a lesson in the past is that it
     * happened. {@code FULL} only means something while a seat could still be taken.
     *
     * <p>The count comes from outside because only {@code booking} can make it; see {@code
     * LessonBookings}.
     */
    public LessonStatus statusAt(Instant now, int confirmedBookings) {
        if (cancelledAt != null) {
            return LessonStatus.CANCELLED;
        }
        if (!period.endsAt().isAfter(now)) {
            return LessonStatus.COMPLETED;
        }
        if (confirmedBookings >= capacity) {
            return LessonStatus.FULL;
        }
        return LessonStatus.OPEN;
    }

    /**
     * Whether the cancellation left the students less than {@link #CANCELLATION_NOTICE}.
     *
     * <p>Derived from the two instants instead of stored: writing it down would be a second copy
     * of a subtraction, and the two copies can only ever agree or be a bug.
     */
    public boolean cancelledAtShortNotice() {
        return cancelledAt != null
                && Duration.between(cancelledAt, period.startsAt()).compareTo(CANCELLATION_NOTICE) < 0;
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public UUID id() {
        return id;
    }

    public UUID teacherUserId() {
        return teacherUserId;
    }

    public LessonType type() {
        return type;
    }

    public Instant startsAt() {
        return period.startsAt();
    }

    public Instant endsAt() {
        return period.endsAt();
    }

    public int capacity() {
        return capacity;
    }

    public String notes() {
        return notes;
    }

    public boolean createdOutsideAvailability() {
        return createdOutsideAvailability;
    }

    public Instant cancelledAt() {
        return cancelledAt;
    }
}

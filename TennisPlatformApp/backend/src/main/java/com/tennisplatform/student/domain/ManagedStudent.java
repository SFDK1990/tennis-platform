package com.tennisplatform.student.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * The relationship that lets a student book: the teacher took them under management.
 *
 * <p>A student may only book while this exists and is {@link ManagedStatus#MANAGED}
 * (01-analisis-funcional.md), so this object - not a role, not a flag on the account - is what
 * gates booking.
 *
 * <p>Deactivating never deletes the row. Two reasons: the history of who was managed when is
 * worth keeping, and the unique index on the pair turns "take them back" into a reactivation
 * of this same row instead of a second relationship that both sides would then have to
 * reconcile.
 *
 * <p>Deactivating also cancels the student's upcoming bookings with this teacher, as
 * 01-analisis-funcional.md requires. That happens in {@code ManageStudentService} through {@code
 * StudentBookings}, not here: this object knows nothing about bookings, and should not.
 */
public class ManagedStudent {

    public static final int MAX_RECORD_TEXT = 2000;

    private final UUID id;
    private final UUID teacherUserId;
    private final UUID studentUserId;
    private ManagedStatus status;
    private Instant managedAt;
    private Instant deactivatedAt;
    // The teacher's record of the student (Fase 23): personal data, emptied when the account goes.
    private UUID levelId;
    private String goals;
    private String privateNotes;

    private ManagedStudent(UUID id, UUID teacherUserId, UUID studentUserId, ManagedStatus status,
                           Instant managedAt, Instant deactivatedAt, UUID levelId, String goals,
                           String privateNotes) {
        this.id = id;
        this.teacherUserId = teacherUserId;
        this.studentUserId = studentUserId;
        this.status = status;
        this.managedAt = managedAt;
        this.deactivatedAt = deactivatedAt;
        this.levelId = levelId;
        this.goals = goals;
        this.privateNotes = privateNotes;
    }

    public static ManagedStudent take(UUID teacherUserId, UUID studentUserId, Instant now) {
        return new ManagedStudent(UUID.randomUUID(), teacherUserId, studentUserId,
                ManagedStatus.MANAGED, now, null, null, null, null);
    }

    /** Rehydration from persistence. Stored values are not re-validated. */
    public static ManagedStudent rehydrate(UUID id, UUID teacherUserId, UUID studentUserId,
                                           ManagedStatus status, Instant managedAt,
                                           Instant deactivatedAt) {
        return rehydrate(id, teacherUserId, studentUserId, status, managedAt, deactivatedAt, null, null, null);
    }

    public static ManagedStudent rehydrate(UUID id, UUID teacherUserId, UUID studentUserId,
                                           ManagedStatus status, Instant managedAt,
                                           Instant deactivatedAt, UUID levelId, String goals,
                                           String privateNotes) {
        return new ManagedStudent(id, teacherUserId, studentUserId, status, managedAt,
                deactivatedAt, levelId, goals, privateNotes);
    }

    /**
     * Replaces the teacher's record of the student: level, goals and private notes. Blank text
     * is stored as nothing; whether the level is one of the teacher's is the use case's check.
     */
    public void updateRecord(UUID levelId, String goals, String privateNotes) {
        this.levelId = levelId;
        this.goals = normalise(goals, "goals");
        this.privateNotes = normalise(privateNotes, "private notes");
    }

    /** The account was deleted: nothing the teacher wrote about the student is kept. */
    public void clearRecord() {
        this.levelId = null;
        this.goals = null;
        this.privateNotes = null;
    }

    private static String normalise(String text, String what) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_RECORD_TEXT) {
            throw new InvalidStudentProfileException("The " + what + " have at most " + MAX_RECORD_TEXT + " characters");
        }
        return trimmed;
    }

    /**
     * Stops the management. Refuses when it is already inactive: the caller believes they are
     * ending something that is running, and answering "done" to that hides a mistake - a stale
     * screen, or the wrong student.
     */
    public void deactivate(Instant now) {
        if (status == ManagedStatus.INACTIVE) {
            throw new StudentAlreadyInactiveException("This student is already deactivated");
        }
        this.status = ManagedStatus.INACTIVE;
        this.deactivatedAt = now;
    }

    /**
     * Takes a previously deactivated student back. {@code managedAt} moves to the new date
     * because it answers "since when is this student managed", and the old answer stopped
     * being true the moment they were deactivated.
     */
    public void reactivate(Instant now) {
        if (status == ManagedStatus.MANAGED) {
            throw new StudentAlreadyManagedException("This student is already managed");
        }
        this.status = ManagedStatus.MANAGED;
        this.managedAt = now;
        this.deactivatedAt = null;
    }

    public boolean isManaged() {
        return status == ManagedStatus.MANAGED;
    }

    public UUID id() {
        return id;
    }

    public UUID teacherUserId() {
        return teacherUserId;
    }

    public UUID studentUserId() {
        return studentUserId;
    }

    public ManagedStatus status() {
        return status;
    }

    public Instant managedAt() {
        return managedAt;
    }

    public Instant deactivatedAt() {
        return deactivatedAt;
    }

    public UUID levelId() {
        return levelId;
    }

    public String goals() {
        return goals;
    }

    public String privateNotes() {
        return privateNotes;
    }
}

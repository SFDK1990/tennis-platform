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

    private final UUID id;
    private final UUID teacherUserId;
    private final UUID studentUserId;
    private ManagedStatus status;
    private Instant managedAt;
    private Instant deactivatedAt;

    private ManagedStudent(UUID id, UUID teacherUserId, UUID studentUserId, ManagedStatus status,
                           Instant managedAt, Instant deactivatedAt) {
        this.id = id;
        this.teacherUserId = teacherUserId;
        this.studentUserId = studentUserId;
        this.status = status;
        this.managedAt = managedAt;
        this.deactivatedAt = deactivatedAt;
    }

    public static ManagedStudent take(UUID teacherUserId, UUID studentUserId, Instant now) {
        return new ManagedStudent(UUID.randomUUID(), teacherUserId, studentUserId,
                ManagedStatus.MANAGED, now, null);
    }

    /** Rehydration from persistence. Stored values are not re-validated. */
    public static ManagedStudent rehydrate(UUID id, UUID teacherUserId, UUID studentUserId,
                                           ManagedStatus status, Instant managedAt,
                                           Instant deactivatedAt) {
        return new ManagedStudent(id, teacherUserId, studentUserId, status, managedAt,
                deactivatedAt);
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
}

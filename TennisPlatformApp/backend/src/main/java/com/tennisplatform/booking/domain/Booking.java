package com.tennisplatform.booking.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A student's seat in a lesson.
 *
 * <p>Two questions are kept apart on purpose: whether the booking still stands ({@link
 * BookingStatus}) and whether the student came ({@link Attendance}). 10-diagrama-er.md folded
 * both into one status, and that would have made marking a student {@code ATTENDED} stop the
 * booking from being {@code CONFIRMED} - dropping it out of the unique index, the overlap
 * constraint and the seat count all at once. See 20-fase9-analisis-booking.md.
 *
 * <p>The lesson's start and end are copied in when the booking is made. The overlap constraint
 * needs them, because an exclusion constraint cannot join another table, and the copy is safe
 * only because a lesson's time cannot be changed. The day it can, that operation has to update
 * these copies in the same transaction. The teacher is copied for the same kind of reason: it is
 * what every teacher-side query filters on, and a lesson never changes teacher.
 *
 * <p>Immutable: every change returns a new booking.
 */
public class Booking {

    /**
     * How much notice a student has to give to cancel. It protects the teacher from a gap they
     * can no longer fill, which is why it binds the student and not the teacher or an admin.
     */
    public static final Duration CANCELLATION_WINDOW = Duration.ofHours(24);

    private final UUID id;
    private final UUID lessonId;
    private final UUID teacherUserId;
    private final UUID studentUserId;
    private final BookingStatus status;
    private final Attendance attendance;
    private final Instant lessonStartsAt;
    private final Instant lessonEndsAt;
    private final Instant bookedAt;
    private final Instant cancelledAt;

    private Booking(UUID id, UUID lessonId, UUID teacherUserId, UUID studentUserId,
                    BookingStatus status, Attendance attendance, Instant lessonStartsAt,
                    Instant lessonEndsAt, Instant bookedAt, Instant cancelledAt) {
        this.id = id;
        this.lessonId = lessonId;
        this.teacherUserId = teacherUserId;
        this.studentUserId = studentUserId;
        this.status = status;
        this.attendance = attendance;
        this.lessonStartsAt = lessonStartsAt;
        this.lessonEndsAt = lessonEndsAt;
        this.bookedAt = bookedAt;
        this.cancelledAt = cancelledAt;
    }

    /** A new, confirmed booking. Whether it may be made at all is the use case's decision. */
    public static Booking confirm(UUID lessonId, UUID teacherUserId, UUID studentUserId,
                                  Instant lessonStartsAt, Instant lessonEndsAt, Instant now) {
        return new Booking(null, lessonId, teacherUserId, studentUserId, BookingStatus.CONFIRMED,
                Attendance.PENDING, lessonStartsAt, lessonEndsAt, now, null);
    }

    /** Rehydration from persistence. Stored values are not re-validated. */
    public static Booking rehydrate(UUID id, UUID lessonId, UUID teacherUserId, UUID studentUserId,
                                    BookingStatus status, Attendance attendance,
                                    Instant lessonStartsAt, Instant lessonEndsAt, Instant bookedAt,
                                    Instant cancelledAt) {
        return new Booking(id, lessonId, teacherUserId, studentUserId, status, attendance,
                lessonStartsAt, lessonEndsAt, bookedAt, cancelledAt);
    }

    /**
     * The student gives up their seat, with at least {@link #CANCELLATION_WINDOW} of notice.
     *
     * <p>Exactly 24 hours is still in time: the rule says "up to 24 hours before", and a limit
     * that rejected its own boundary would be stricter than what the product wrote down.
     */
    public Booking cancelByStudent(Instant now) {
        requireCancellable(now);
        if (Duration.between(now, lessonStartsAt).compareTo(CANCELLATION_WINDOW) < 0) {
            throw new CancellationWindowExpiredException(
                    "A booking can be cancelled up to 24 hours before the lesson starts");
        }
        return cancelled(BookingStatus.CANCELLED_BY_STUDENT, now);
    }

    /** The teacher cancels a booking in one of their lessons. No window: see {@link #CANCELLATION_WINDOW}. */
    public Booking cancelByTeacher(Instant now) {
        requireCancellable(now);
        return cancelled(BookingStatus.CANCELLED_BY_TEACHER, now);
    }

    /** An admin resolves an incident. No window, as 01-analisis-funcional.md settled. */
    public Booking cancelByAdmin(Instant now) {
        requireCancellable(now);
        return cancelled(BookingStatus.CANCELLED_BY_ADMIN, now);
    }

    /**
     * Records whether the student came.
     *
     * <p>Only once the lesson has started, as 01-analisis-funcional.md §11 says: before that,
     * "did not show up" is not yet a fact. It can be changed afterwards as often as needed, since
     * ticking the wrong box is exactly the kind of mistake that has to be undoable.
     */
    public Booking markAttendance(Attendance marked, Instant now) {
        if (status != BookingStatus.CONFIRMED) {
            throw new BookingAlreadyCancelledException(
                    "Attendance is only recorded for bookings that still stand");
        }
        if (now.isBefore(lessonStartsAt)) {
            throw new AttendanceNotYetOpenException(
                    "Attendance can be recorded once the lesson has started");
        }
        return new Booking(id, lessonId, teacherUserId, studentUserId, status, marked,
                lessonStartsAt, lessonEndsAt, bookedAt, cancelledAt);
    }

    /**
     * A booking in a lesson that has started is not cancelled but marked: from that moment on
     * what happened is attendance, and cancelling would rewrite it.
     */
    private void requireCancellable(Instant now) {
        if (status != BookingStatus.CONFIRMED) {
            throw new BookingAlreadyCancelledException("The booking was already cancelled");
        }
        if (!now.isBefore(lessonStartsAt)) {
            throw new LessonAlreadyStartedException(
                    "The lesson has already started; the booking can no longer be cancelled");
        }
    }

    private Booking cancelled(BookingStatus reason, Instant now) {
        return new Booking(id, lessonId, teacherUserId, studentUserId, reason, attendance,
                lessonStartsAt, lessonEndsAt, bookedAt, now);
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public UUID id() {
        return id;
    }

    public UUID lessonId() {
        return lessonId;
    }

    public UUID teacherUserId() {
        return teacherUserId;
    }

    public UUID studentUserId() {
        return studentUserId;
    }

    public BookingStatus status() {
        return status;
    }

    public Attendance attendance() {
        return attendance;
    }

    public Instant lessonStartsAt() {
        return lessonStartsAt;
    }

    public Instant lessonEndsAt() {
        return lessonEndsAt;
    }

    public Instant bookedAt() {
        return bookedAt;
    }

    public Instant cancelledAt() {
        return cancelledAt;
    }
}

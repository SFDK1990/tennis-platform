package com.tennisplatform.booking.adapters.out.persistence;

import com.tennisplatform.booking.domain.Attendance;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
class BookingEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "lesson_id", nullable = false, updatable = false)
    private UUID lessonId;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Column(name = "student_user_id", nullable = false, updatable = false)
    private UUID studentUserId;

    /** STRING, not ORDINAL: the column has a CHECK on the literals. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance", nullable = false, length = 10)
    private Attendance attendance;

    @Column(name = "lesson_starts_at", nullable = false, updatable = false)
    private Instant lessonStartsAt;

    @Column(name = "lesson_ends_at", nullable = false, updatable = false)
    private Instant lessonEndsAt;

    @Column(name = "booked_at", nullable = false, updatable = false)
    private Instant bookedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BookingEntity() {
    }

    static BookingEntity fromDomain(Booking booking) {
        BookingEntity entity = new BookingEntity();
        entity.id = booking.id();
        entity.lessonId = booking.lessonId();
        entity.teacherUserId = booking.teacherUserId();
        entity.studentUserId = booking.studentUserId();
        entity.lessonStartsAt = booking.lessonStartsAt();
        entity.lessonEndsAt = booking.lessonEndsAt();
        entity.bookedAt = booking.bookedAt();
        entity.apply(booking);
        return entity;
    }

    /** Copies the half that can change onto an entity that may already be managed. */
    void apply(Booking booking) {
        this.status = booking.status();
        this.attendance = booking.attendance();
        this.cancelledAt = booking.cancelledAt();
    }

    Booking toDomain() {
        return Booking.rehydrate(id, lessonId, teacherUserId, studentUserId, status, attendance,
                lessonStartsAt, lessonEndsAt, bookedAt, cancelledAt);
    }
}

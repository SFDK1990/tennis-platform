package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.domain.BookingStatus;
import com.tennisplatform.student.application.port.spi.StudentBookings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code student}'s request to cancel the upcoming bookings of a student it has let go.
 *
 * <p>{@code CANCELLED_BY_TEACHER} when the teacher ended the relationship, {@code CANCELLED_BY_ADMIN}
 * when an administrator disabled the account, {@code CANCELLED_BY_STUDENT} when the student deleted
 * it. No 24-hour window in any case: it protects the teacher from a change of mind, and none of
 * these is one.
 */
public class BookingsOfStudents implements StudentBookings {

    private final BookingRepository bookings;
    private final BookingEvents events;

    public BookingsOfStudents(BookingRepository bookings, ApplicationEventPublisher publisher) {
        this.bookings = bookings;
        this.events = new BookingEvents(publisher);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelUpcomingWith(UUID teacherUserId, UUID studentUserId, Instant at) {
        events.cancelled(bookings.cancelUpcoming(teacherUserId, studentUserId, BookingStatus.CANCELLED_BY_TEACHER, at),
                BookingStatus.CANCELLED_BY_TEACHER, null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelUpcomingOfDisabledAccount(UUID teacherUserId, UUID studentUserId, Instant at) {
        events.cancelled(bookings.cancelUpcoming(teacherUserId, studentUserId, BookingStatus.CANCELLED_BY_ADMIN, at),
                BookingStatus.CANCELLED_BY_ADMIN, null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelUpcomingOfDeletedAccount(UUID teacherUserId, UUID studentUserId, Instant at) {
        // Nobody is told: the address is about to be wiped, and the student is the one leaving.
        bookings.cancelUpcoming(teacherUserId, studentUserId, BookingStatus.CANCELLED_BY_STUDENT, at);
    }
}

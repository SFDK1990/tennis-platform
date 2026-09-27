package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.domain.BookingStatus;
import com.tennisplatform.student.application.port.spi.StudentBookings;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code student}'s request to cancel the upcoming bookings of a student it has let go.
 *
 * <p>{@code CANCELLED_BY_TEACHER} when the teacher ended the relationship, {@code CANCELLED_BY_ADMIN}
 * when an administrator disabled the account. No 24-hour window in either case: it protects the
 * teacher, and neither of them is a student changing their mind.
 */
public class BookingsOfStudents implements StudentBookings {

    private final BookingRepository bookings;

    public BookingsOfStudents(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelUpcomingWith(UUID teacherUserId, UUID studentUserId, Instant at) {
        bookings.cancelUpcoming(teacherUserId, studentUserId, BookingStatus.CANCELLED_BY_TEACHER, at);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelUpcomingOfDisabledAccount(UUID teacherUserId, UUID studentUserId, Instant at) {
        bookings.cancelUpcoming(teacherUserId, studentUserId, BookingStatus.CANCELLED_BY_ADMIN, at);
    }
}

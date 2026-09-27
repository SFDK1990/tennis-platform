package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.in.CancelBooking;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingNotFoundException;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Predicate;

public class CancelBookingService implements CancelBooking {

    private static final Logger log = LoggerFactory.getLogger(CancelBookingService.class);

    private final BookingRepository bookings;
    private final BookingViews views;
    private final Clock clock;

    public CancelBookingService(BookingRepository bookings, GetLesson getLesson, Clock clock) {
        this.bookings = bookings;
        this.views = new BookingViews(getLesson);
        this.clock = clock;
    }

    @Override
    @Transactional
    public BookingView asStudent(UUID studentUserId, UUID bookingId) {
        Booking booking = find(bookingId, b -> b.studentUserId().equals(studentUserId));
        BookingView cancelled = views.of(bookings.save(booking.cancelByStudent(clock.instant())));
        log.info("Booking {} cancelled by the student", bookingId);
        return cancelled;
    }

    @Override
    @Transactional
    public BookingView asTeacher(UUID teacherUserId, UUID bookingId) {
        Booking booking = find(bookingId, b -> b.teacherUserId().equals(teacherUserId));
        BookingView cancelled = views.of(bookings.save(booking.cancelByTeacher(clock.instant())));
        log.info("Booking {} cancelled by the teacher", bookingId);
        return cancelled;
    }

    @Override
    @Transactional
    public BookingView asAdmin(UUID bookingId) {
        Booking booking = find(bookingId, b -> true);
        BookingView cancelled = views.of(bookings.save(booking.cancelByAdmin(clock.instant())));
        log.info("Booking {} cancelled by the admin", bookingId);
        return cancelled;
    }

    /**
     * A booking the caller may not touch answers exactly like one that does not exist, as Fase 8
     * did with another teacher's lesson: telling a caller that an id exists but is not theirs is
     * telling them something they had no way to know.
     */
    private Booking find(UUID bookingId, Predicate<Booking> visibleToCaller) {
        return bookings.findById(bookingId)
                .filter(visibleToCaller)
                .orElseThrow(() -> new BookingNotFoundException("No booking with id " + bookingId));
    }
}

package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.in.CancelBooking;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingNotFoundException;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LockLesson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/** Giving up a seat, which hands it to the lesson's waiting list (Fase 21). */
public class CancelBookingService implements CancelBooking {

    private static final Logger log = LoggerFactory.getLogger(CancelBookingService.class);

    private final BookingRepository bookings;
    private final BookingViews views;
    private final LockLesson lockLesson;
    private final WaitingListPromotion promotion;
    private final Clock clock;
    private final BookingEvents events;

    public CancelBookingService(BookingRepository bookings, GetLesson getLesson, LockLesson lockLesson,
                                WaitingListPromotion promotion, Clock clock, ApplicationEventPublisher publisher) {
        this.bookings = bookings;
        this.views = new BookingViews(getLesson);
        this.lockLesson = lockLesson;
        this.promotion = promotion;
        this.clock = clock;
        this.events = new BookingEvents(publisher);
    }

    @Override
    @Transactional
    public BookingView asStudent(UUID studentUserId, UUID bookingId) {
        BookingView cancelled = cancel(find(bookingId, b -> b.studentUserId().equals(studentUserId)),
                booking -> booking.cancelByStudent(clock.instant()));
        log.info("Booking {} cancelled by the student", bookingId);
        return cancelled;
    }

    @Override
    @Transactional
    public BookingView asTeacher(UUID teacherUserId, UUID bookingId) {
        BookingView cancelled = cancel(find(bookingId, b -> b.teacherUserId().equals(teacherUserId)),
                booking -> booking.cancelByTeacher(clock.instant()));
        log.info("Booking {} cancelled by the teacher", bookingId);
        return cancelled;
    }

    @Override
    @Transactional
    public BookingView asAdmin(UUID bookingId) {
        BookingView cancelled = cancel(find(bookingId, b -> true), booking -> booking.cancelByAdmin(clock.instant()));
        log.info("Booking {} cancelled by the admin", bookingId);
        return cancelled;
    }

    /**
     * The hand-over runs in this transaction and under the lesson's lock: a student booking at
     * the same moment counts the seat as still taken until it is somebody else's, and two
     * cancellations in one lesson give their seats out one after the other.
     */
    private BookingView cancel(Booking booking, UnaryOperator<Booking> how) {
        Booking saved = bookings.save(how.apply(booking));
        events.cancelled(saved, saved.status(), null);
        promotion.fillFreeSeats(lockLesson.lockForBooking(booking.lessonId()));
        return views.of(saved);
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

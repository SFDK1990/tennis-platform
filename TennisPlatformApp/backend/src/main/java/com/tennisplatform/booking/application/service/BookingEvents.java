package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.in.BookingCancelled;
import com.tennisplatform.booking.application.port.in.BookingConfirmed;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingStatus;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collection;

/** Tells the rest of the application what happened to a booking (Fase 20). */
final class BookingEvents {

    private final ApplicationEventPublisher publisher;

    BookingEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    void confirmed(Booking booking) {
        publisher.publishEvent(new BookingConfirmed(booking.id(), booking.lessonId(), booking.teacherUserId(),
                booking.studentUserId(), booking.lessonStartsAt(), booking.lessonEndsAt()));
    }

    void cancelled(Booking booking, BookingStatus as, String reason) {
        publisher.publishEvent(new BookingCancelled(booking.id(), booking.lessonId(), booking.teacherUserId(),
                booking.studentUserId(), booking.lessonStartsAt(), booking.lessonEndsAt(), as.name(), reason));
    }

    void cancelled(Collection<Booking> bookings, BookingStatus as, String reason) {
        bookings.forEach(booking -> cancelled(booking, as, reason));
    }
}

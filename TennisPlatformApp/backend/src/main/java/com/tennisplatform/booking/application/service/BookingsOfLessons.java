package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.port.out.WaitingListRepository;
import com.tennisplatform.booking.domain.BookingStatus;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.spi.LessonBookings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * {@code lesson}'s questions about bookings, answered by the module that has them.
 *
 * <p>Both methods join the caller's transaction and refuse to run without one. The count has to
 * see what the caller's lock protects, and the cascade has to succeed or fail together with the
 * cancellation of the lesson; neither is true of a transaction of its own.
 */
public class BookingsOfLessons implements LessonBookings {

    private final BookingRepository bookings;
    private final WaitingListRepository waiting;
    private final WaitingListPromotion promotion;
    private final BookingEvents events;

    public BookingsOfLessons(BookingRepository bookings, WaitingListRepository waiting,
                             WaitingListPromotion promotion, ApplicationEventPublisher publisher) {
        this.bookings = bookings;
        this.waiting = waiting;
        this.promotion = promotion;
        this.events = new BookingEvents(publisher);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Map<UUID, Integer> countConfirmed(Collection<UUID> lessonIds) {
        if (lessonIds.isEmpty()) {
            return Map.of();
        }
        return bookings.countConfirmed(lessonIds);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelAllOf(UUID lessonId, Instant cancelledAt, String reason) {
        events.cancelled(bookings.cancelConfirmedOfLesson(lessonId, BookingStatus.CANCELLED_BY_TEACHER, cancelledAt),
                BookingStatus.CANCELLED_BY_TEACHER, reason);
        waiting.deleteAllOfLesson(lessonId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelAllOfCancelledByAdmin(UUID lessonId, Instant cancelledAt) {
        events.cancelled(bookings.cancelConfirmedOfLesson(lessonId, BookingStatus.CANCELLED_BY_ADMIN, cancelledAt),
                BookingStatus.CANCELLED_BY_ADMIN, null);
        waiting.deleteAllOfLesson(lessonId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void fillFreeSeats(LessonView lesson) {
        promotion.fillFreeSeats(lesson);
    }
}

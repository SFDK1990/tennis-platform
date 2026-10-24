package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.port.out.WaitingListRepository;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingStatus;
import com.tennisplatform.lesson.application.port.in.LockLesson;
import com.tennisplatform.student.application.port.spi.StudentBookings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code student}'s request to cancel the upcoming bookings of a student it has let go.
 *
 * <p>{@code CANCELLED_BY_TEACHER} when the teacher ended the relationship, {@code CANCELLED_BY_ADMIN}
 * when an administrator disabled the account, {@code CANCELLED_BY_STUDENT} when the student deleted
 * it. No 24-hour window in any case: it protects the teacher from a change of mind, and none of
 * these is one.
 *
 * <p>Each freed seat goes to the lesson's waiting list, and the student leaves every list of this
 * teacher (Fase 21). The lessons are locked one by one, always in the same order, so two of these
 * running at once cannot each hold a lock the other is waiting for.
 */
public class BookingsOfStudents implements StudentBookings {

    private final BookingRepository bookings;
    private final WaitingListRepository waiting;
    private final LockLesson lockLesson;
    private final WaitingListPromotion promotion;
    private final BookingEvents events;

    public BookingsOfStudents(BookingRepository bookings, WaitingListRepository waiting, LockLesson lockLesson,
                              WaitingListPromotion promotion, ApplicationEventPublisher publisher) {
        this.bookings = bookings;
        this.waiting = waiting;
        this.lockLesson = lockLesson;
        this.promotion = promotion;
        this.events = new BookingEvents(publisher);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelUpcomingWith(UUID teacherUserId, UUID studentUserId, Instant at) {
        cancelUpcoming(teacherUserId, studentUserId, BookingStatus.CANCELLED_BY_TEACHER, at, true);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelUpcomingOfDisabledAccount(UUID teacherUserId, UUID studentUserId, Instant at) {
        cancelUpcoming(teacherUserId, studentUserId, BookingStatus.CANCELLED_BY_ADMIN, at, true);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelUpcomingOfDeletedAccount(UUID teacherUserId, UUID studentUserId, Instant at) {
        // Nobody is told: the address is about to be wiped, and the student is the one leaving.
        cancelUpcoming(teacherUserId, studentUserId, BookingStatus.CANCELLED_BY_STUDENT, at, false);
    }

    private void cancelUpcoming(UUID teacherUserId, UUID studentUserId, BookingStatus as, Instant at,
                                boolean tellTheStudent) {
        List<UUID> lessons = bookings.upcomingLessonIds(teacherUserId, studentUserId, at).stream()
                .distinct()
                .sorted()
                .toList();
        List<Booking> cancelled = bookings.cancelUpcoming(teacherUserId, studentUserId, as, at);
        if (tellTheStudent) {
            events.cancelled(cancelled, as, null);
        }
        waiting.deleteAllOfStudentWithTeacher(teacherUserId, studentUserId);
        lessons.forEach(lessonId -> promotion.fillFreeSeats(lockLesson.lockForBooking(lessonId)));
    }
}

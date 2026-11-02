package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.in.GroupBookings;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.port.out.WaitingListRepository;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.LevelNotAllowedException;
import com.tennisplatform.booking.domain.StudentScheduleOverlapException;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.in.LockLesson;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The seats of a fixed group (43-fase28-analisis-mensualidades.md). The lessons are locked in id
 * order, the same order {@link BookingsOfStudents} takes them in, so two of these, or one of these
 * and a deactivation, cannot each hold a lock the other is waiting for.
 */
public class GroupBookingsService implements GroupBookings {

    private static final Logger log = LoggerFactory.getLogger(GroupBookingsService.class);

    private static final String CANCELLED = "CANCELLED";

    private final BookingRepository bookings;
    private final WaitingListRepository waiting;
    private final LockLesson lockLesson;
    private final QueryManagedStudent managedStudents;
    private final WaitingListPromotion promotion;
    private final Clock clock;

    public GroupBookingsService(BookingRepository bookings, WaitingListRepository waiting, LockLesson lockLesson,
                                QueryManagedStudent managedStudents, WaitingListPromotion promotion, Clock clock) {
        this.bookings = bookings;
        this.waiting = waiting;
        this.lockLesson = lockLesson;
        this.managedStudents = managedStudents;
        this.promotion = promotion;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Instant> bookSeats(UUID teacherUserId, UUID studentUserId, UUID membershipId,
                                   Collection<UUID> lessonIds) {
        if (!managedStudents.isManagedBy(teacherUserId, studentUserId)) {
            throw new ForbiddenOperationException("STUDENT_NOT_MANAGED", "The teacher of this group does not manage you");
        }
        Instant now = clock.instant();
        List<LessonView> toBook = new ArrayList<>();
        List<Instant> full = new ArrayList<>();
        for (UUID lessonId : lessonIds.stream().distinct().sorted().toList()) {
            LessonView lesson = lockLesson.lockForBooking(lessonId);
            if (CANCELLED.equals(lesson.status()) || !lesson.startsAt().isAfter(now)
                    || bookings.existsConfirmed(lessonId, studentUserId)) {
                continue;
            }
            if (!LevelRule.allows(managedStudents, lesson, studentUserId)) {
                throw new LevelNotAllowedException("This group is for another level");
            }
            if (lesson.bookedCount() >= lesson.capacity()) {
                full.add(lesson.startsAt());
                continue;
            }
            toBook.add(lesson);
        }
        if (!full.isEmpty()) {
            return full.stream().sorted().toList();
        }
        for (LessonView lesson : toBook) {
            if (bookings.existsOverlappingConfirmed(studentUserId, lesson.startsAt(), lesson.endsAt())) {
                throw new StudentScheduleOverlapException("You already have a lesson at the time of one of the group's");
            }
            bookings.save(Booking.confirmForGroup(lesson.id(), teacherUserId, studentUserId, lesson.startsAt(),
                    lesson.endsAt(), now, membershipId));
            // A seat now held cannot be waited for.
            waiting.delete(lesson.id(), studentUserId);
        }
        log.info("Membership {} holds {} new seats", membershipId, toBook.size());
        return List.of();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelSeats(UUID membershipId, Instant from, boolean byStudent) {
        Instant now = clock.instant();
        Instant since = from.isAfter(now) ? from : now;
        List<Booking> seats = bookings.findConfirmedOfMembershipFrom(membershipId, since).stream()
                .sorted((one, other) -> one.lessonId().compareTo(other.lessonId()))
                .toList();
        for (Booking seat : seats) {
            bookings.save(seat.cancelOnLeavingGroup(byStudent, now));
            // Locked after the seat is given up, like a single cancellation: the count read under the
            // lock is the one the waiting list fills.
            promotion.fillFreeSeats(lockLesson.lockForBooking(seat.lessonId()));
        }
        log.info("Membership {} gave up {} seats", membershipId, seats.size());
    }
}

package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.in.WaitingPlaceLost;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.port.out.WaitingListRepository;
import com.tennisplatform.booking.application.port.spi.GroupSeats;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.WaitingListEntry;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.pass.application.port.in.ChargePasses;
import com.tennisplatform.pass.application.port.in.ChargePasses.PassCharge;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Hands the free seats of a lesson to the first students on its waiting list
 * (34-fase21-analisis-lista-espera.md, decision 2).
 *
 * <p>It runs inside the transaction that freed the seat, and that transaction holds the lesson's
 * lock: a student booking at that moment waits at the lock and finds the lesson full again. Done
 * after the commit instead, the seat would be free for an instant, and whoever came in then would
 * take it from the student who had been waiting.
 */
public class WaitingListPromotion {

    private static final Logger log = LoggerFactory.getLogger(WaitingListPromotion.class);

    private static final String CANCELLED = "CANCELLED";

    private final BookingRepository bookings;
    private final WaitingListRepository waiting;
    private final QueryManagedStudent managedStudents;
    private final ChargePasses passes;
    private final GroupSeats groupSeats;
    private final Clock clock;
    private final BookingEvents events;
    private final ApplicationEventPublisher publisher;

    public WaitingListPromotion(BookingRepository bookings, WaitingListRepository waiting,
                                QueryManagedStudent managedStudents, ChargePasses passes, GroupSeats groupSeats,
                                Clock clock, ApplicationEventPublisher publisher) {
        this.bookings = bookings;
        this.waiting = waiting;
        this.managedStudents = managedStudents;
        this.passes = passes;
        this.groupSeats = groupSeats;
        this.clock = clock;
        this.events = new BookingEvents(publisher);
        this.publisher = publisher;
    }

    /**
     * {@code lesson} has to be read after its lock was taken, in the caller's transaction, so its
     * seat count is still true. Less than 24 hours before the start the list is closed and the
     * seat stays free for whoever books it (decision 4).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void fillFreeSeats(LessonView lesson) {
        Instant now = clock.instant();
        if (CANCELLED.equals(lesson.status()) || !WaitingListEntry.isOpen(lesson.startsAt(), now)) {
            return;
        }
        int free = lesson.capacity() - lesson.bookedCount();
        for (WaitingListEntry entry : waiting.findByLesson(lesson.id())) {
            if (free <= 0) {
                return;
            }
            waiting.delete(entry.lessonId(), entry.studentUserId());
            if (!canTakeTheSeat(entry, lesson)) {
                log.info("Waiting student {} skipped in lesson {}: they can no longer book it",
                        entry.studentUserId(), lesson.id());
                continue;
            }
            Booking seat = seatFor(entry, lesson, now);
            if (seat == null) {
                log.info("Waiting student {} skipped in lesson {}: no lessons left on their passes",
                        entry.studentUserId(), lesson.id());
                publisher.publishEvent(new WaitingPlaceLost(lesson.id(), lesson.teacherUserId(), entry.studentUserId()));
                continue;
            }
            Booking saved = bookings.save(seat);
            log.info("Booking {} created in lesson {} for student {} from the waiting list",
                    saved.id(), lesson.id(), entry.studentUserId());
            events.confirmedFromWaitingList(saved);
            free--;
        }
    }

    /**
     * The seat as the group's when the student is a member of the lesson's group (Fase 28), and
     * otherwise paid with a pass; null when no pass can pay. Paying is asked last and only of
     * whoever would get the seat: it locks their passes.
     */
    private Booking seatFor(WaitingListEntry entry, LessonView lesson, Instant now) {
        if (lesson.seriesId() != null) {
            var membership = groupSeats.membershipPaying(entry.studentUserId(), lesson.seriesId(), lesson.startsAt());
            if (membership.isPresent()) {
                return Booking.confirmForGroup(lesson.id(), lesson.teacherUserId(), entry.studentUserId(),
                        lesson.startsAt(), lesson.endsAt(), now, membership.get());
            }
        }
        PassCharge charge = passes.charge(lesson.teacherUserId(), entry.studentUserId(), lesson.type(),
                lesson.startsAt());
        if (charge.refused()) {
            return null;
        }
        return Booking.confirm(lesson.id(), lesson.teacherUserId(), entry.studentUserId(), lesson.startsAt(),
                lesson.endsAt(), now, charge.passId());
    }

    /**
     * What booking would check, asked again now: since they joined, the student may have booked
     * another lesson at that time, stopped being the teacher's student, or been put on another
     * level than the lesson's (Fase 23).
     */
    private boolean canTakeTheSeat(WaitingListEntry entry, LessonView lesson) {
        return managedStudents.isManagedBy(lesson.teacherUserId(), entry.studentUserId())
                && LevelRule.allows(managedStudents, lesson, entry.studentUserId())
                && !bookings.existsConfirmed(lesson.id(), entry.studentUserId())
                && !bookings.existsOverlappingConfirmed(entry.studentUserId(), lesson.startsAt(), lesson.endsAt());
    }
}

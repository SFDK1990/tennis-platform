package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.in.BookLesson;
import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingAlreadyExistsException;
import com.tennisplatform.booking.domain.EmailNotVerifiedException;
import com.tennisplatform.booking.domain.LessonAlreadyStartedException;
import com.tennisplatform.booking.domain.LessonFullException;
import com.tennisplatform.booking.domain.LessonNotBookableException;
import com.tennisplatform.booking.domain.LevelNotAllowedException;
import com.tennisplatform.booking.domain.NoPassBalanceException;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.booking.domain.StudentScheduleOverlapException;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.in.LockLesson;
import com.tennisplatform.pass.application.port.in.ChargePasses;
import com.tennisplatform.pass.application.port.in.ChargePasses.PassCharge;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Taking a seat, which is the one operation in the MVP where two requests genuinely race.
 *
 * <p>The whole method is one transaction, and its first step is to lock the lesson. From then on
 * any other booking of the same lesson waits at that lock, so the seat count this request reads
 * is still true when it inserts. Everything that can be checked is checked after the lock, for
 * the same reason: a check made before it could be stale by the time the insert happens.
 */
public class BookLessonService implements BookLesson {

    private static final Logger log = LoggerFactory.getLogger(BookLessonService.class);

    private static final String CANCELLED = "CANCELLED";
    private static final String FULL = "FULL";

    private final BookingRepository bookings;
    private final LockLesson lockLesson;
    private final GetLesson getLesson;
    private final QueryManagedStudent managedStudents;
    private final ChargePasses passes;
    private final Clock clock;
    private final BookingEvents events;

    public BookLessonService(BookingRepository bookings, LockLesson lockLesson, GetLesson getLesson,
                             QueryManagedStudent managedStudents, ChargePasses passes, Clock clock,
                             ApplicationEventPublisher publisher) {
        this.bookings = bookings;
        this.lockLesson = lockLesson;
        this.getLesson = getLesson;
        this.managedStudents = managedStudents;
        this.passes = passes;
        this.clock = clock;
        this.events = new BookingEvents(publisher);
    }

    @Override
    @Transactional
    public BookingView book(UUID studentUserId, boolean emailVerified, UUID lessonId) {
        if (!emailVerified) {
            throw new EmailNotVerifiedException("Verify your email address before booking a lesson");
        }

        LessonView lesson = lockLesson.lockForBooking(lessonId);

        // Checked against the database on every request, not against the token: an access token
        // outlives the deactivation of the student it was issued to by several minutes.
        if (!managedStudents.isManagedBy(lesson.teacherUserId(), studentUserId)) {
            throw new ForbiddenOperationException("STUDENT_NOT_MANAGED", "The teacher of this lesson does not manage you");
        }
        if (!LevelRule.allows(managedStudents, lesson, studentUserId)) {
            throw new LevelNotAllowedException("This lesson is for another level");
        }
        if (CANCELLED.equals(lesson.status())) {
            throw new LessonNotBookableException("The lesson was cancelled");
        }
        Instant now = clock.instant();
        if (!lesson.startsAt().isAfter(now)) {
            throw new LessonAlreadyStartedException("The lesson has already started");
        }

        // Before the seat count, so a student who already holds a seat in a lesson that is now
        // full hears that they are in, not that there is no room.
        if (bookings.existsConfirmed(lessonId, studentUserId)) {
            throw new BookingAlreadyExistsException("You already have a seat in this lesson");
        }
        if (FULL.equals(lesson.status())) {
            throw new LessonFullException("Every seat of this lesson is taken");
        }

        // Checked here for the message, and enforced again by the exclusion constraint: the lock
        // above serialises bookings of this lesson, not two bookings of the same student in two
        // different lessons, which can still race each other.
        if (bookings.existsOverlappingConfirmed(studentUserId, lesson.startsAt(), lesson.endsAt())) {
            throw new StudentScheduleOverlapException("You already have a lesson at that time");
        }

        // Last, because it locks the student's passes: every other reason to refuse is cheaper, and
        // a student told "no lessons left" should not then learn the lesson was full anyway.
        PassCharge charge = passes.charge(lesson.teacherUserId(), studentUserId, lesson.type(), lesson.startsAt());
        if (charge.refused()) {
            throw new NoPassBalanceException("You have no lessons left on a pass for this lesson");
        }

        Booking saved = bookings.save(Booking.confirm(lessonId, lesson.teacherUserId(), studentUserId,
                lesson.startsAt(), lesson.endsAt(), now, charge.passId()));
        log.info("Booking {} created in lesson {} for student {}", saved.id(), lessonId, studentUserId);
        events.confirmed(saved);

        // Read again rather than adjusted by hand, so whether the lesson is now FULL is decided
        // in one place only - Lesson.statusAt - and the count includes the row just inserted.
        return BookingView.from(saved, getLesson.byId(lessonId));
    }
}

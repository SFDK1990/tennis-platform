package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.in.WaitingList;
import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.port.out.WaitingListRepository;
import com.tennisplatform.booking.application.port.spi.GroupSeats;
import com.tennisplatform.booking.domain.AlreadyWaitingException;
import com.tennisplatform.booking.domain.BookingAlreadyExistsException;
import com.tennisplatform.booking.domain.EmailNotVerifiedException;
import com.tennisplatform.booking.domain.LessonAlreadyStartedException;
import com.tennisplatform.booking.domain.LessonNotBookableException;
import com.tennisplatform.booking.domain.LessonNotFullException;
import com.tennisplatform.booking.domain.LevelNotAllowedException;
import com.tennisplatform.booking.domain.NoPassBalanceException;
import com.tennisplatform.booking.domain.StudentScheduleOverlapException;
import com.tennisplatform.booking.domain.WaitingListClosedException;
import com.tennisplatform.booking.domain.WaitingListEntry;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.in.LockLesson;
import com.tennisplatform.pass.application.port.in.ChargePasses;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Joining, leaving and reading waiting lists. Joining locks the lesson, like booking: "it is
 * full" is only true while nobody else can take or free a seat.
 */
public class WaitingListService implements WaitingList {

    private static final Logger log = LoggerFactory.getLogger(WaitingListService.class);

    private static final String CANCELLED = "CANCELLED";
    private static final String FULL = "FULL";

    private final WaitingListRepository waiting;
    private final BookingRepository bookings;
    private final LockLesson lockLesson;
    private final GetLesson getLesson;
    private final QueryManagedStudent managedStudents;
    private final ChargePasses passes;
    private final GroupSeats groupSeats;
    private final Clock clock;

    public WaitingListService(WaitingListRepository waiting, BookingRepository bookings, LockLesson lockLesson,
                              GetLesson getLesson, QueryManagedStudent managedStudents, ChargePasses passes,
                              GroupSeats groupSeats, Clock clock) {
        this.waiting = waiting;
        this.bookings = bookings;
        this.lockLesson = lockLesson;
        this.getLesson = getLesson;
        this.managedStudents = managedStudents;
        this.passes = passes;
        this.groupSeats = groupSeats;
        this.clock = clock;
    }

    @Override
    @Transactional
    public WaitingPlace join(UUID studentUserId, boolean emailVerified, UUID lessonId) {
        if (!emailVerified) {
            throw new EmailNotVerifiedException("Verify your email address before joining a waiting list");
        }
        LessonView lesson = lockLesson.lockForBooking(lessonId);
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
        if (!WaitingListEntry.isOpen(lesson.startsAt(), now)) {
            throw new WaitingListClosedException("The waiting list closes 24 hours before the lesson");
        }
        if (bookings.existsConfirmed(lessonId, studentUserId)) {
            throw new BookingAlreadyExistsException("You already have a seat in this lesson");
        }
        if (!FULL.equals(lesson.status())) {
            throw new LessonNotFullException("There is a free seat: book it instead");
        }
        if (waiting.exists(lessonId, studentUserId)) {
            throw new AlreadyWaitingException("You are already on this lesson's waiting list");
        }
        if (bookings.existsOverlappingConfirmed(studentUserId, lesson.startsAt(), lesson.endsAt())) {
            throw new StudentScheduleOverlapException("You already have a lesson at that time");
        }
        // Asked, not charged: the lesson is spent if and when the seat comes (decision 5). A member of
        // the lesson's group needs no pass: the fee pays for the seat (Fase 28).
        if (!paidByGroup(lesson, studentUserId)
                && !passes.canPay(lesson.teacherUserId(), studentUserId, lesson.type(), lesson.startsAt())) {
            throw new NoPassBalanceException("You have no lessons left on a pass for this lesson");
        }
        WaitingListEntry saved = waiting.save(WaitingListEntry.join(lessonId, lesson.teacherUserId(), studentUserId, now));
        log.info("Student {} joined the waiting list of lesson {}", studentUserId, lessonId);
        return new WaitingPlace(lesson, positionOf(lessonId, studentUserId), saved.joinedAt());
    }

    @Override
    @Transactional
    public void leave(UUID studentUserId, UUID lessonId) {
        waiting.delete(lessonId, studentUserId);
        log.info("Student {} left the waiting list of lesson {}", studentUserId, lessonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitingPlace> forStudent(UUID studentUserId) {
        List<WaitingListEntry> entries = waiting.findByStudent(studentUserId);
        if (entries.isEmpty()) {
            return List.of();
        }
        Instant now = clock.instant();
        Map<UUID, LessonView> lessons = getLesson.byIds(entries.stream().map(WaitingListEntry::lessonId).toList())
                .stream()
                .collect(Collectors.toMap(LessonView::id, Function.identity()));
        return entries.stream()
                .filter(entry -> lessons.containsKey(entry.lessonId()) && isOpen(lessons.get(entry.lessonId()), now))
                .map(entry -> new WaitingPlace(lessons.get(entry.lessonId()),
                        positionOf(entry.lessonId(), studentUserId), entry.joinedAt()))
                .sorted(Comparator.comparing(place -> place.lesson().startsAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitingStudent> forTeacher(UUID teacherUserId, UUID lessonId) {
        List<WaitingListEntry> entries = waiting.findByLesson(lessonId);
        if (entries.isEmpty() || !entries.getFirst().teacherUserId().equals(teacherUserId)) {
            return List.of();
        }
        return IntStream.range(0, entries.size())
                .mapToObj(i -> new WaitingStudent(entries.get(i).studentUserId(), i + 1, entries.get(i).joinedAt()))
                .toList();
    }

    /** A list past its closing point stays in the table but is no longer shown (decision 4). */
    private static boolean isOpen(LessonView lesson, Instant now) {
        return !CANCELLED.equals(lesson.status()) && WaitingListEntry.isOpen(lesson.startsAt(), now);
    }

    private int positionOf(UUID lessonId, UUID studentUserId) {
        List<WaitingListEntry> list = waiting.findByLesson(lessonId);
        return IntStream.range(0, list.size())
                .filter(i -> list.get(i).studentUserId().equals(studentUserId))
                .findFirst()
                .orElseThrow() + 1;
    }

    private boolean paidByGroup(LessonView lesson, UUID studentUserId) {
        return lesson.seriesId() != null
                && groupSeats.membershipPaying(studentUserId, lesson.seriesId(), lesson.startsAt()).isPresent();
    }
}

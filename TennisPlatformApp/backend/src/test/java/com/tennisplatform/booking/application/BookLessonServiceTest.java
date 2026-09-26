package com.tennisplatform.booking.application;

import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.application.service.BookLessonService;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingAlreadyExistsException;
import com.tennisplatform.booking.domain.EmailNotVerifiedException;
import com.tennisplatform.booking.domain.LessonAlreadyStartedException;
import com.tennisplatform.booking.domain.LessonFullException;
import com.tennisplatform.booking.domain.LessonNotBookableException;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.booking.domain.StudentScheduleOverlapException;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.in.LockLesson;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BookLessonServiceTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID LESSON_ID = UUID.randomUUID();
    private static final Instant STARTS = Instant.parse("2026-06-01T09:00:00Z");
    private static final Instant ENDS = Instant.parse("2026-06-01T10:00:00Z");
    private static final Instant NOW = STARTS.minusSeconds(3 * 86_400);

    private BookingRepository bookings;
    private LockLesson lockLesson;
    private GetLesson getLesson;
    private QueryManagedStudent managedStudents;
    private BookLessonService service;

    @BeforeEach
    void setUp() {
        bookings = mock(BookingRepository.class);
        lockLesson = mock(LockLesson.class);
        getLesson = mock(GetLesson.class);
        managedStudents = mock(QueryManagedStudent.class);

        when(managedStudents.isManagedBy(TEACHER_ID, STUDENT_ID)).thenReturn(true);
        when(bookings.save(any())).thenAnswer(call -> call.getArgument(0));
        when(getLesson.byId(LESSON_ID)).thenReturn(lesson("OPEN", STARTS, 1));

        service = new BookLessonService(bookings, lockLesson, getLesson, managedStudents,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void confirmsASeatCopyingTheLessonsTimeAndTeacher() {
        when(lockLesson.lockForBooking(LESSON_ID)).thenReturn(lesson("OPEN", STARTS, 0));

        service.book(STUDENT_ID, true, LESSON_ID);

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookings).save(saved.capture());
        assertThat(saved.getValue().teacherUserId()).isEqualTo(TEACHER_ID);
        assertThat(saved.getValue().lessonStartsAt()).isEqualTo(STARTS);
        assertThat(saved.getValue().lessonEndsAt()).isEqualTo(ENDS);
        assertThat(saved.getValue().bookedAt()).isEqualTo(NOW);
    }

    /**
     * The lock comes first and everything else after it: a check made before the lock could be
     * stale by the time the insert happens, which is the whole race this service exists to stop.
     */
    @Test
    void locksTheLessonBeforeCheckingAnything() {
        when(lockLesson.lockForBooking(LESSON_ID)).thenReturn(lesson("OPEN", STARTS, 0));

        service.book(STUDENT_ID, true, LESSON_ID);

        InOrder order = inOrder(lockLesson, managedStudents, bookings);
        order.verify(lockLesson).lockForBooking(LESSON_ID);
        order.verify(managedStudents).isManagedBy(TEACHER_ID, STUDENT_ID);
        order.verify(bookings).existsConfirmed(LESSON_ID, STUDENT_ID);
        order.verify(bookings).save(any());
    }

    @Test
    void refusesAStudentWhoHasNotVerifiedTheirAddressWithoutTakingTheLock() {
        assertThatThrownBy(() -> service.book(STUDENT_ID, false, LESSON_ID))
                .isInstanceOf(EmailNotVerifiedException.class);

        verifyNoInteractions(lockLesson);
    }

    @Test
    void refusesAStudentTheTeacherDoesNotManage() {
        when(lockLesson.lockForBooking(LESSON_ID)).thenReturn(lesson("OPEN", STARTS, 0));
        when(managedStudents.isManagedBy(TEACHER_ID, STUDENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.book(STUDENT_ID, true, LESSON_ID))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(bookings, never()).save(any());
    }

    @Test
    void refusesACancelledLesson() {
        when(lockLesson.lockForBooking(LESSON_ID)).thenReturn(lesson("CANCELLED", STARTS, 0));

        assertThatThrownBy(() -> service.book(STUDENT_ID, true, LESSON_ID))
                .isInstanceOf(LessonNotBookableException.class);
    }

    /** The boundary is the start itself: a lesson starting right now has started. */
    @Test
    void refusesALessonThatStartsNow() {
        when(lockLesson.lockForBooking(LESSON_ID)).thenReturn(lesson("OPEN", NOW, 0));

        assertThatThrownBy(() -> service.book(STUDENT_ID, true, LESSON_ID))
                .isInstanceOf(LessonAlreadyStartedException.class);
    }

    @Test
    void refusesAFullLesson() {
        when(lockLesson.lockForBooking(LESSON_ID)).thenReturn(lesson("FULL", STARTS, 1));

        assertThatThrownBy(() -> service.book(STUDENT_ID, true, LESSON_ID))
                .isInstanceOf(LessonFullException.class);
        verify(bookings, never()).save(any());
    }

    /** A student who already holds the last seat hears that they are in, not that there is no room. */
    @Test
    void aDuplicateIsReportedAsSuchEvenWhenTheLessonIsFull() {
        when(lockLesson.lockForBooking(LESSON_ID)).thenReturn(lesson("FULL", STARTS, 1));
        when(bookings.existsConfirmed(LESSON_ID, STUDENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.book(STUDENT_ID, true, LESSON_ID))
                .isInstanceOf(BookingAlreadyExistsException.class);
    }

    @Test
    void refusesABookingThatOverlapsAnotherOneOfTheStudent() {
        when(lockLesson.lockForBooking(LESSON_ID)).thenReturn(lesson("OPEN", STARTS, 0));
        when(bookings.existsOverlappingConfirmed(STUDENT_ID, STARTS, ENDS)).thenReturn(true);

        assertThatThrownBy(() -> service.book(STUDENT_ID, true, LESSON_ID))
                .isInstanceOf(StudentScheduleOverlapException.class);
        verify(bookings, never()).save(any());
    }

    private static LessonView lesson(String status, Instant startsAt, int booked) {
        return new LessonView(LESSON_ID, TEACHER_ID, "INDIVIDUAL", startsAt, ENDS, 1, booked, status,
                null, false, null, false);
    }
}

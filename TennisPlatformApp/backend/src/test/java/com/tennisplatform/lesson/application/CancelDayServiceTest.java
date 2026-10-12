package com.tennisplatform.lesson.application;

import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.application.port.spi.LessonBookings;
import com.tennisplatform.lesson.application.service.CancelLessonService;
import com.tennisplatform.lesson.domain.DateInThePastException;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cancelling a rainy day (32-fase20-analisis-avisos-calendario.md, decision 4), around the night
 * Madrid moves from UTC+1 to UTC+2: 28 March 2027 at 01:00 UTC. Dates are literal on purpose.
 */
class CancelDayServiceTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final LocalDate THE_DAY = LocalDate.parse("2027-03-28");
    /** Midnight in Madrid, still on UTC+1. */
    private static final Instant DAY_STARTS = Instant.parse("2027-03-27T23:00:00Z");
    /** The next midnight, already on UTC+2: the day lasts 23 hours. */
    private static final Instant DAY_ENDS = Instant.parse("2027-03-28T22:00:00Z");

    private LessonRepository lessons;
    private LessonBookings bookings;
    private GetTeacherProfile teacherProfile;

    @BeforeEach
    void setUp() {
        lessons = mock(LessonRepository.class);
        bookings = mock(LessonBookings.class);
        teacherProfile = mock(GetTeacherProfile.class);
        when(teacherProfile.byUserId(TEACHER_ID)).thenReturn(Optional.of(
                new TeacherProfileView(TEACHER_ID, "Ana Serrano", null, "Europe/Madrid")));
        when(lessons.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private CancelLessonService at(String now) {
        return new CancelLessonService(lessons, bookings, teacherProfile, Clock.fixed(Instant.parse(now), ZoneOffset.UTC));
    }

    private static Lesson lesson(String startsAt, Instant cancelledAt) {
        Instant starts = Instant.parse(startsAt);
        return Lesson.rehydrate(UUID.randomUUID(), TEACHER_ID, LessonType.GROUP, starts, starts.plusSeconds(3600), 4,
                null, false, cancelledAt);
    }

    @Test
    void theDayIsTheTeachersAlsoOnTheNightTheClocksGoForward() {
        Lesson justAfterMidnight = lesson("2027-03-27T23:30:00Z", null);   // 00:30 in Madrid, the 28th
        Lesson lateEvening = lesson("2027-03-28T21:00:00Z", null);         // 23:00 in Madrid, the 28th
        Lesson theNightBefore = lesson("2027-03-27T22:30:00Z", null);      // 23:30 in Madrid, the 27th
        when(lessons.findByTeacherBetween(TEACHER_ID, DAY_STARTS, DAY_ENDS))
                .thenReturn(List.of(theNightBefore, justAfterMidnight, lateEvening));

        List<LessonView> cancelled = at("2027-03-27T12:00:00Z").cancelDay(TEACHER_ID, THE_DAY, "Lluvia");

        assertThat(cancelled).extracting(LessonView::id).containsExactly(justAfterMidnight.id(), lateEvening.id());
        verify(bookings).cancelAllOf(justAfterMidnight.id(), Instant.parse("2027-03-27T12:00:00Z"), "Lluvia");
        verify(bookings).cancelAllOf(lateEvening.id(), Instant.parse("2027-03-27T12:00:00Z"), "Lluvia");
        verify(bookings, never()).cancelAllOf(eq(theNightBefore.id()), any(), anyString());
    }

    @Test
    void theLessonThatAlreadyStartedAndTheOneAlreadyCancelledAreLeftAlone() {
        Lesson started = lesson("2027-03-28T09:30:00Z", null);
        Lesson alreadyCancelled = lesson("2027-03-28T15:00:00Z", Instant.parse("2027-03-20T10:00:00Z"));
        Lesson later = lesson("2027-03-28T17:00:00Z", null);
        when(lessons.findByTeacherBetween(TEACHER_ID, DAY_STARTS, DAY_ENDS))
                .thenReturn(List.of(started, alreadyCancelled, later));

        List<LessonView> cancelled = at("2027-03-28T10:00:00Z").cancelDay(TEACHER_ID, THE_DAY, null);

        assertThat(cancelled).extracting(LessonView::id).containsExactly(later.id());
    }

    @Test
    void aDayGoneByInMadridIsRefusedAlthoughItIsStillThatDayInUtc() {
        // 00:30 of the 28th in Madrid, 23:30 of the 27th in UTC.
        CancelLessonService service = at("2027-03-27T23:30:00Z");

        assertThatThrownBy(() -> service.cancelDay(TEACHER_ID, LocalDate.parse("2027-03-27"), null))
                .isInstanceOf(DateInThePastException.class);
        assertThat(service.cancelDay(TEACHER_ID, THE_DAY, null)).isEmpty();
    }
}

package com.tennisplatform.calendar.application;

import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.calendar.application.port.in.GetMonthSummary.LevelFigures;
import com.tennisplatform.calendar.application.port.in.GetMonthSummary.MonthFigures;
import com.tennisplatform.calendar.application.port.in.GetMonthSummary.MonthSummaryView;
import com.tennisplatform.calendar.application.service.GetMonthSummaryService;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.shared.domain.DateRange.InvalidDateRangeException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The monthly summary (38-fase25-analisis-resumen-mes.md) on a fixed clock and literal dates:
 * 10 November 2026 at noon, with Madrid on UTC+1, so 00:30 on the 1st is still 31 October in UTC.
 */
class GetMonthSummaryServiceTest {

    private static final UUID TEACHER = UUID.randomUUID();
    private static final UUID LEVEL = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    private final List<LessonView> lessonsOfTheTeacher = new ArrayList<>();
    private final List<BookingView> confirmed = new ArrayList<>();
    private GetLesson lessons;
    private GetBookings bookings;
    private GetTeacherProfile teachers;

    @BeforeEach
    void setUp() {
        lessons = mock(GetLesson.class);
        bookings = mock(GetBookings.class);
        teachers = mock(GetTeacherProfile.class);
        when(teachers.byUserId(TEACHER)).thenReturn(Optional.of(
                new TeacherProfileView(TEACHER, "Ana Serrano", null, "Europe/Madrid")));
        // Like the real one: every lesson that touches the dates, on the teacher's clock.
        when(lessons.forTeacherBetween(any(), any(), any())).thenAnswer(call -> {
            LocalDate from = call.getArgument(1);
            LocalDate to = call.getArgument(2);
            Instant start = from.atStartOfDay(ZoneOffset.ofHours(1)).toInstant();
            Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.ofHours(1)).toInstant();
            return lessonsOfTheTeacher.stream()
                    .filter(lesson -> lesson.startsAt().isBefore(end) && lesson.endsAt().isAfter(start))
                    .toList();
        });
        when(bookings.confirmedInLessons(any())).thenAnswer(call -> {
            Collection<UUID> ids = call.getArgument(0);
            return confirmed.stream().filter(seat -> ids.contains(seat.lessonId())).toList();
        });
    }

    private MonthSummaryView summary(YearMonth month) {
        return new GetMonthSummaryService(teachers, lessons, bookings, Clock.fixed(NOW, ZoneOffset.UTC))
                .forTeacher(TEACHER, month);
    }

    private UUID lesson(String startsAt, String type, int capacity, UUID level, boolean cancelled, boolean shortNotice) {
        Instant starts = Instant.parse(startsAt);
        UUID id = UUID.randomUUID();
        lessonsOfTheTeacher.add(new LessonView(id, TEACHER, type, starts, starts.plusSeconds(3600), capacity, 0,
                cancelled ? "CANCELLED" : "COMPLETED", null, false, cancelled ? starts.minusSeconds(60) : null,
                shortNotice, null, null, null, level, null));
        return id;
    }

    private void seat(UUID lesson, UUID student, String attendance) {
        confirmed.add(new BookingView(UUID.randomUUID(), lesson, student, "CONFIRMED", attendance,
                NOW.minusSeconds(86_400 * 30L), null, null));
    }

    @Test
    void theMonthIsCountedOnTheTeachersClockUpToNowWithTheMonthBeforeAndEachLevel() {
        UUID lucia = UUID.randomUUID();
        UUID pablo = UUID.randomUUID();
        UUID marta = UUID.randomUUID();
        UUID ines = UUID.randomUUID();

        // November: 00:30 on the 1st in Madrid, a group of the level, with each kind of register.
        UUID firstOfNovember = lesson("2026-10-31T23:30:00Z", "GROUP", 4, LEVEL, false, false);
        seat(firstOfNovember, lucia, "ATTENDED");
        seat(firstOfNovember, pablo, "NO_SHOW");
        seat(firstOfNovember, marta, "PENDING");
        UUID individual = lesson("2026-11-05T10:00:00Z", "INDIVIDUAL", 1, null, false, false);
        seat(individual, lucia, "ATTENDED");
        lesson("2026-11-06T10:00:00Z", "GROUP", 4, null, true, true);       // rain, on the day
        lesson("2026-11-20T10:00:00Z", "GROUP", 4, LEVEL, false, false);    // still to come
        // October: 23:30 on the 31st in Madrid, and a lesson cancelled well ahead.
        UUID lastOfOctober = lesson("2026-10-31T22:30:00Z", "GROUP", 2, LEVEL, false, false);
        seat(lastOfOctober, ines, "ATTENDED");
        lesson("2026-10-12T10:00:00Z", "GROUP", 4, LEVEL, true, false);

        MonthSummaryView summary = summary(YearMonth.parse("2026-11"));

        assertThat(summary.month()).isEqualTo("2026-11");
        assertThat(summary.timezone()).isEqualTo("Europe/Madrid");
        assertThat(summary.current()).isEqualTo(new MonthFigures(2, 1, 1, 5, 4, 2, 1, 1, 3, 1, 1));
        assertThat(summary.previous()).isEqualTo(new MonthFigures(1, 0, 1, 2, 1, 1, 0, 0, 1, 1, 0));
        assertThat(summary.levels()).extracting(LevelFigures::levelId).containsExactlyInAnyOrder(LEVEL, null);
        LevelFigures ofTheLevel = summary.levels().stream().filter(row -> LEVEL.equals(row.levelId())).findFirst()
                .orElseThrow();
        assertThat(ofTheLevel.current()).isEqualTo(new MonthFigures(1, 0, 1, 4, 3, 1, 1, 1, 3, 0, 0));
        assertThat(summary.levels()).extracting(row -> row.current().lessonsGiven()).containsOnly(1, 1);
    }

    @Test
    void withoutAMonthItIsTheCurrentOneAndAMonthStillToComeIsRefused() {
        assertThat(summary(null).month()).isEqualTo("2026-11");
        assertThat(summary(null).current()).isEqualTo(new MonthFigures(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThatThrownBy(() -> summary(YearMonth.parse("2026-12"))).isInstanceOf(InvalidDateRangeException.class);
    }
}

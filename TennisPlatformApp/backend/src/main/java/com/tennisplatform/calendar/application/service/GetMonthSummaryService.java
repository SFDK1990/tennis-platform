package com.tennisplatform.calendar.application.service;

import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.calendar.application.port.in.GetMonthSummary;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.pass.application.port.in.QueryPasses;
import com.tennisplatform.pass.application.port.in.QueryPasses.PassSales;
import com.tennisplatform.shared.domain.DateRange.InvalidDateRangeException;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Counts the month out of the lessons and their confirmed bookings, every time it is asked: with
 * one teacher a month is a few dozen lessons, and a correction to the register shows at once.
 *
 * <p>A month is the teacher's, on their wall clock: a lesson at 00:30 on the 1st in Madrid belongs
 * to that month even though in UTC it is still the month before. The current month is counted up
 * to now, so the lessons still to come do not count as given.
 */
public class GetMonthSummaryService implements GetMonthSummary {

    private static final String CANCELLED = "CANCELLED";
    private static final String INDIVIDUAL = "INDIVIDUAL";
    private static final String ATTENDED = "ATTENDED";
    private static final String NO_SHOW = "NO_SHOW";

    private final GetTeacherProfile teacherProfiles;
    private final GetLesson lessons;
    private final GetBookings bookings;
    private final QueryPasses passes;
    private final Clock clock;

    public GetMonthSummaryService(GetTeacherProfile teacherProfiles, GetLesson lessons, GetBookings bookings,
                                  QueryPasses passes, Clock clock) {
        this.teacherProfiles = teacherProfiles;
        this.lessons = lessons;
        this.bookings = bookings;
        this.passes = passes;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthSummaryView forTeacher(UUID teacherUserId, YearMonth month) {
        ZoneId zone = teacherProfiles.byUserId(teacherUserId)
                .map(profile -> ZoneId.of(profile.timezone()))
                .orElseThrow(() -> ForbiddenOperationException.teacherOnly("Only a teacher has a summary"));
        Instant now = clock.instant();
        YearMonth current = YearMonth.from(now.atZone(zone));
        YearMonth asked = month == null ? current : month;
        if (asked.isAfter(current)) {
            throw new InvalidDateRangeException("The month " + asked + " has not started yet");
        }

        List<LessonView> thisMonth = lessonsOf(teacherUserId, asked, zone, now);
        List<LessonView> monthBefore = lessonsOf(teacherUserId, asked.minusMonths(1), zone, now);
        Map<UUID, List<BookingView>> seats = seatsOf(thisMonth, monthBefore);

        Set<UUID> levels = new LinkedHashSet<>();
        thisMonth.forEach(lesson -> levels.add(lesson.levelId()));
        monthBefore.forEach(lesson -> levels.add(lesson.levelId()));
        List<LevelFigures> byLevel = new ArrayList<>();
        for (UUID level : levels) {
            byLevel.add(new LevelFigures(level,
                    tally(thisMonth.stream().filter(lesson -> Objects.equals(level, lesson.levelId())).toList(), seats),
                    tally(monthBefore.stream().filter(lesson -> Objects.equals(level, lesson.levelId())).toList(),
                            seats)));
        }

        return new MonthSummaryView(asked.toString(), zone.getId(), tally(thisMonth, seats), tally(monthBefore, seats),
                byLevel, salesOf(teacherUserId, asked), salesOf(teacherUserId, asked.minusMonths(1)));
    }

    private Sales salesOf(UUID teacherUserId, YearMonth month) {
        PassSales sold = passes.soldBetween(teacherUserId, month.atDay(1), month.plusMonths(1).atDay(1));
        return new Sales(sold.count(), sold.totalCents());
    }

    /** The lessons that start within the month on the teacher's clock, and before now. */
    private List<LessonView> lessonsOf(UUID teacherUserId, YearMonth month, ZoneId zone, Instant now) {
        Instant start = month.atDay(1).atStartOfDay(zone).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        Instant until = end.isBefore(now) ? end : now;
        // The range also brings a lesson of the last night of the month before that ends after midnight.
        return lessons.forTeacherBetween(teacherUserId, month.atDay(1), month.atEndOfMonth()).stream()
                .filter(lesson -> !lesson.startsAt().isBefore(start) && lesson.startsAt().isBefore(until))
                .toList();
    }

    private Map<UUID, List<BookingView>> seatsOf(Collection<LessonView> one, Collection<LessonView> other) {
        List<UUID> given = new ArrayList<>();
        one.stream().filter(GetMonthSummaryService::given).map(LessonView::id).forEach(given::add);
        other.stream().filter(GetMonthSummaryService::given).map(LessonView::id).forEach(given::add);
        return bookings.confirmedInLessons(given).stream().collect(Collectors.groupingBy(BookingView::lessonId));
    }

    private static boolean given(LessonView lesson) {
        return !CANCELLED.equals(lesson.status());
    }

    private static MonthFigures tally(List<LessonView> month, Map<UUID, List<BookingView>> seats) {
        int given = 0;
        int individual = 0;
        int offered = 0;
        int taken = 0;
        int attended = 0;
        int noShow = 0;
        int unmarked = 0;
        int cancelled = 0;
        int shortNotice = 0;
        Set<UUID> students = new HashSet<>();
        for (LessonView lesson : month) {
            if (!given(lesson)) {
                cancelled++;
                if (lesson.cancelledAtShortNotice()) {
                    shortNotice++;
                }
                continue;
            }
            given++;
            if (INDIVIDUAL.equals(lesson.type())) {
                individual++;
            }
            offered += lesson.capacity();
            for (BookingView seat : seats.getOrDefault(lesson.id(), List.of())) {
                taken++;
                students.add(seat.studentUserId());
                switch (seat.attendance()) {
                    case ATTENDED -> attended++;
                    case NO_SHOW -> noShow++;
                    default -> unmarked++;
                }
            }
        }
        return new MonthFigures(given, individual, given - individual, offered, taken, attended, noShow, unmarked,
                students.size(), cancelled, shortNotice);
    }
}

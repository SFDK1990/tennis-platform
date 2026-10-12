package com.tennisplatform.calendar.application.service;

import com.tennisplatform.availability.application.port.in.QueryAvailability;
import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.calendar.application.port.in.CalendarView;
import com.tennisplatform.calendar.application.port.in.CalendarView.CalendarLesson;
import com.tennisplatform.calendar.application.port.in.CalendarView.Interval;
import com.tennisplatform.calendar.application.port.in.CalendarView.MyBooking;
import com.tennisplatform.calendar.application.port.in.GetCalendar;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.shared.domain.DateRange;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Puts together what other modules already know. It decides nothing about lessons, bookings or
 * availability - every rule stays where it lives - which is what keeps it a read-only view.
 */
public class GetCalendarService implements GetCalendar {

    private static final String CANCELLED = "CANCELLED";

    private final GetTeacherProfile teacherProfiles;
    private final QueryAvailability availability;
    private final GetLesson lessons;
    private final QueryManagedStudent managedStudents;
    private final GetBookings bookings;

    public GetCalendarService(GetTeacherProfile teacherProfiles, QueryAvailability availability,
                              GetLesson lessons, QueryManagedStudent managedStudents, GetBookings bookings) {
        this.teacherProfiles = teacherProfiles;
        this.availability = availability;
        this.lessons = lessons;
        this.managedStudents = managedStudents;
        this.bookings = bookings;
    }

    @Override
    @Transactional(readOnly = true)
    public CalendarView forTeacher(UUID teacherUserId, LocalDate from, LocalDate to) {
        DateRange range = new DateRange(from, to);
        ZoneId zone = zoneOf(teacherUserId);
        List<Interval> intervals = availability.intervals(teacherUserId,
                        range.from().atStartOfDay(zone).toInstant(),
                        range.to().plusDays(1).atStartOfDay(zone).toInstant()).stream()
                .map(interval -> new Interval(interval.startsAt(), interval.endsAt()))
                .toList();
        List<CalendarLesson> lessonsOfRange = lessons.forTeacherBetween(teacherUserId, from, to).stream()
                .map(lesson -> toCalendar(lesson, null))
                .toList();
        return new CalendarView(zone.getId(), intervals, lessonsOfRange);
    }

    @Override
    @Transactional(readOnly = true)
    public CalendarView forStudent(UUID studentUserId, LocalDate from, LocalDate to) {
        new DateRange(from, to);
        List<UUID> teachers = managedStudents.teachersOf(studentUserId);
        if (teachers.isEmpty()) {
            return CalendarView.empty();
        }

        List<LessonView> found = new ArrayList<>();
        for (UUID teacher : teachers) {
            found.addAll(lessons.forTeacherBetween(teacher, from, to));
        }
        Map<UUID, BookingView> mine = latestBookingPerLesson(studentUserId, found);

        List<CalendarLesson> visible = found.stream()
                .filter(lesson -> !CANCELLED.equals(lesson.status()) || mine.containsKey(lesson.id()))
                .sorted(Comparator.comparing(LessonView::startsAt))
                .map(lesson -> toCalendar(lesson, mine.get(lesson.id())))
                .toList();
        return new CalendarView(zoneOf(teachers.get(0)).getId(), List.of(), visible);
    }

    /** A student may have cancelled and booked again: the last booking is the one that tells the story. */
    private Map<UUID, BookingView> latestBookingPerLesson(UUID studentUserId, List<LessonView> found) {
        return bookings.ofStudentInLessons(studentUserId, found.stream().map(LessonView::id).toList())
                .stream()
                .collect(Collectors.toMap(BookingView::lessonId, Function.identity(),
                        (earlier, later) -> later));
    }

    @Override
    @Transactional(readOnly = true)
    public CalendarView forAdministration(LocalDate from, LocalDate to) {
        return forTeacher(teacherProfiles.get().userId(), from, to);
    }

    private ZoneId zoneOf(UUID teacherUserId) {
        return teacherProfiles.byUserId(teacherUserId)
                .map(profile -> ZoneId.of(profile.timezone()))
                .orElseThrow(() -> ForbiddenOperationException.teacherOnly("Only a teacher has a calendar of their own"));
    }

    private static CalendarLesson toCalendar(LessonView lesson, BookingView booking) {
        MyBooking myBooking = booking == null ? null
                : new MyBooking(booking.id(), booking.status(), booking.attendance());
        return new CalendarLesson(lesson.id(), lesson.teacherUserId(), lesson.type(), lesson.startsAt(),
                lesson.endsAt(), lesson.capacity(), lesson.bookedCount(), lesson.status(), myBooking);
    }
}

package com.tennisplatform.calendar.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code timezone} is the teacher's, which is how the days of the range were read. It is null
 * only for a student nobody manages yet, whose calendar is empty.
 */
public record CalendarView(String timezone, List<Interval> availability, List<CalendarLesson> lessons) {

    public CalendarView {
        availability = List.copyOf(availability);
        lessons = List.copyOf(lessons);
    }

    public static CalendarView empty() {
        return new CalendarView(null, List.of(), List.of());
    }

    public record Interval(Instant startsAt, Instant endsAt) {
    }

    /** A lesson and, for a student, their own booking in it ({@code myBooking} null otherwise). */
    public record CalendarLesson(UUID id, UUID teacherUserId, String type, Instant startsAt, Instant endsAt,
                                 int capacity, int bookedCount, String status, MyBooking myBooking) {
    }

    public record MyBooking(UUID id, String status, String attendance) {
    }
}

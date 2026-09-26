package com.tennisplatform.lesson.application.service;

import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.application.port.spi.LessonBookings;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.shared.domain.DateRange;
import com.tennisplatform.lesson.domain.LessonNotFoundException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GetLessonService implements GetLesson {

    private final LessonRepository lessons;
    private final LessonBookings bookings;
    private final TeacherLessons teacher;
    private final Clock clock;

    public GetLessonService(LessonRepository lessons, LessonBookings bookings,
                            GetTeacherProfile teacherProfile, Clock clock) {
        this.lessons = lessons;
        this.bookings = bookings;
        this.teacher = new TeacherLessons(teacherProfile);
        this.clock = clock;
    }

    /**
     * Readable by any authenticated caller, like the teacher's availability: a student has to be
     * able to see what they would be signing up for, and a lesson holds nobody's personal data.
     * The one field that is the teacher's alone, {@code notes}, is filtered at the web edge,
     * which is where the caller's role is known.
     */
    @Override
    @Transactional(readOnly = true)
    public LessonView byId(UUID id) {
        Lesson lesson = lessons.findById(id)
                .orElseThrow(() -> new LessonNotFoundException("No lesson with id " + id));
        return viewsOf(List.of(lesson)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonView> byIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return viewsOf(lessons.findAllById(ids));
    }

    /**
     * The dates are turned into instants with the teacher's own zone, because "the lessons of
     * the 3rd" means the 3rd where the teacher lives. The upper bound is the start of the day
     * after the last one, so a lesson at 23:30 on the final date is included.
     *
     * <p>{@code atStartOfDay} is what handles the day the clocks go forward, when midnight may
     * not exist: it moves to the first instant that does, instead of producing a time that never
     * happened.
     */
    @Override
    @Transactional(readOnly = true)
    public List<LessonView> forTeacherBetween(UUID teacherUserId, LocalDate from, LocalDate to) {
        ZoneId zone = teacher.requireTheTeacher(teacherUserId);
        DateRange range = new DateRange(from, to);

        Instant start = range.from().atStartOfDay(zone).toInstant();
        Instant end = range.to().plusDays(1).atStartOfDay(zone).toInstant();
        return viewsOf(lessons.findByTeacherBetween(teacherUserId, start, end));
    }

    /** One count for the whole batch, however many lessons it holds. */
    private List<LessonView> viewsOf(List<Lesson> found) {
        Instant now = clock.instant();
        Map<UUID, Integer> booked = bookings.countConfirmed(found.stream().map(Lesson::id).toList());
        return found.stream()
                .map(lesson -> LessonView.from(lesson, now, booked.getOrDefault(lesson.id(), 0)))
                .toList();
    }
}

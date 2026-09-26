package com.tennisplatform.lesson.application.service;

import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.domain.LessonDateRange;
import com.tennisplatform.lesson.domain.LessonNotFoundException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class GetLessonService implements GetLesson {

    private final LessonRepository lessons;
    private final TeacherLessons teacher;
    private final Clock clock;

    public GetLessonService(LessonRepository lessons, GetTeacherProfile teacherProfile, Clock clock) {
        this.lessons = lessons;
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
        Instant now = clock.instant();
        return lessons.findById(id)
                .map(lesson -> LessonView.from(lesson, now))
                .orElseThrow(() -> new LessonNotFoundException("No lesson with id " + id));
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
        LessonDateRange range = new LessonDateRange(from, to);

        Instant start = range.from().atStartOfDay(zone).toInstant();
        Instant end = range.to().plusDays(1).atStartOfDay(zone).toInstant();
        Instant now = clock.instant();

        return lessons.findByTeacherBetween(teacherUserId, start, end).stream()
                .map(lesson -> LessonView.from(lesson, now))
                .toList();
    }
}

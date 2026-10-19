package com.tennisplatform.lesson.application.service;

import com.tennisplatform.lesson.application.port.in.CancelLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.application.port.spi.LessonBookings;
import com.tennisplatform.lesson.domain.DateInThePastException;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonNotFoundException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CancelLessonService implements CancelLesson {

    private static final Logger log = LoggerFactory.getLogger(CancelLessonService.class);

    private final LessonRepository lessons;
    private final LessonBookings bookings;
    private final TeacherLessons teacher;
    private final Clock clock;

    public CancelLessonService(LessonRepository lessons, LessonBookings bookings,
                               GetTeacherProfile teacherProfile, Clock clock) {
        this.lessons = lessons;
        this.bookings = bookings;
        this.teacher = new TeacherLessons(teacherProfile);
        this.clock = clock;
    }

    @Override
    @Transactional
    public LessonView cancel(UUID teacherUserId, UUID lessonId, String reason) {
        teacher.requireTheTeacher(teacherUserId);

        Lesson lesson = lessons.findById(lessonId).orElseThrow(() -> notFound(lessonId));

        // Somebody else's lesson answers 404 rather than 403: with a single teacher the case is
        // unreachable, and the day it is not, telling a caller that an id exists but is not
        // theirs is telling them something they had no way to know.
        if (!lesson.teacherUserId().equals(teacherUserId)) {
            throw notFound(lessonId);
        }

        Instant now = clock.instant();
        Lesson cancelled = lessons.save(lesson.cancel(now));

        // Same transaction on purpose: if the bookings cannot be cancelled, the lesson is not
        // cancelled either, and nobody is left holding a seat in a lesson that no longer runs.
        bookings.cancelAllOf(cancelled.id(), now, reason);
        log.info("Lesson {} cancelled with its bookings", lessonId);
        return LessonView.from(cancelled, now, 0);
    }

    @Override
    @Transactional
    public List<LessonView> cancelDay(UUID teacherUserId, LocalDate day, String reason) {
        ZoneId zone = teacher.requireTheTeacher(teacherUserId);
        Instant now = clock.instant();
        if (day.isBefore(LocalDate.ofInstant(now, zone))) {
            throw new DateInThePastException("The day " + day + " has already gone by");
        }

        // The day as the teacher lives it: a lesson at 00:30 in Madrid belongs to it although in
        // UTC it starts the evening before.
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
        List<LessonView> cancelled = new ArrayList<>();
        for (Lesson lesson : lessons.findByTeacherBetween(teacherUserId, from, to)) {
            boolean startsThatDay = !lesson.startsAt().isBefore(from) && lesson.startsAt().isBefore(to);
            if (startsThatDay && lesson.cancelledAt() == null && lesson.startsAt().isAfter(now)) {
                Lesson saved = lessons.save(lesson.cancel(now));
                bookings.cancelAllOf(saved.id(), now, reason);
                cancelled.add(LessonView.from(saved, now, 0));
            }
        }
        log.info("Day {} cancelled: {} lessons", day, cancelled.size());
        return cancelled;
    }

    @Override
    @Transactional
    public LessonView cancelAsAdmin(UUID lessonId) {
        Lesson lesson = lessons.findById(lessonId).orElseThrow(() -> notFound(lessonId));

        Instant now = clock.instant();
        Lesson cancelled = lessons.save(lesson.cancel(now));
        bookings.cancelAllOfCancelledByAdmin(cancelled.id(), now);
        log.info("Lesson {} cancelled by an admin with its bookings", lessonId);
        return LessonView.from(cancelled, now, 0);
    }

    private static LessonNotFoundException notFound(UUID lessonId) {
        return new LessonNotFoundException("No lesson with id " + lessonId);
    }
}

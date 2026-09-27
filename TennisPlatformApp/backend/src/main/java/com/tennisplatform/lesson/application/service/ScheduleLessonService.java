package com.tennisplatform.lesson.application.service;

import com.tennisplatform.availability.application.port.in.QueryAvailability;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.in.NewLesson;
import com.tennisplatform.lesson.application.port.in.ScheduleLesson;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonInThePastException;
import com.tennisplatform.lesson.domain.LessonOutsideAvailabilityException;
import com.tennisplatform.lesson.domain.LessonOverlapException;
import com.tennisplatform.lesson.domain.LessonPeriod;
import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.platform.application.port.in.GetMaxGroupCapacity;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

public class ScheduleLessonService implements ScheduleLesson {

    private static final Logger log = LoggerFactory.getLogger(ScheduleLessonService.class);

    private final LessonRepository lessons;
    private final TeacherLessons teacher;
    private final QueryAvailability availability;
    private final GetMaxGroupCapacity groupCapacity;
    private final Clock clock;

    public ScheduleLessonService(LessonRepository lessons, GetTeacherProfile teacherProfile,
                                 QueryAvailability availability, GetMaxGroupCapacity groupCapacity,
                                 Clock clock) {
        this.lessons = lessons;
        this.teacher = new TeacherLessons(teacherProfile);
        this.availability = availability;
        this.groupCapacity = groupCapacity;
        this.clock = clock;
    }

    @Override
    @Transactional
    public LessonView schedule(UUID teacherUserId, NewLesson request) {
        ZoneId zone = teacher.requireTheTeacher(teacherUserId);
        LessonType type = LessonType.parse(request.type());
        LessonPeriod period = new LessonPeriod(request.startsAt(), request.endsAt());
        Instant now = clock.instant();
        if (!period.startsAt().isAfter(now)) {
            throw new LessonInThePastException("A lesson has to start after the moment it is created");
        }

        boolean covered = availability.covers(teacherUserId, period.startsAt(), period.endsAt());
        if (!covered && !request.overrideAvailability()) {
            throw new LessonOutsideAvailabilityException(
                    "The lesson falls outside the configured availability. Change the hours or "
                            + "ask for it explicitly");
        }

        // Checked here so the answer says something useful, and enforced again by the exclusion
        // constraint in the adapter, because between this read and the write another request
        // fits - the teacher with two tabs open is enough.
        if (lessons.existsOverlapping(teacherUserId, period.startsAt(), period.endsAt())) {
            throw new LessonOverlapException("Another lesson already runs at that time");
        }

        // The cap only constrains group lessons, so an individual one does not pay for a read
        // of a configuration value it cannot use. One is what an individual lesson holds anyway.
        int maxGroupCapacity = type == LessonType.GROUP ? groupCapacity.maxGroupCapacity() : 1;

        // Recorded as what happened, not as what was asked for: a lesson is outside the hours
        // when it is outside them, whether or not the caller expected it to be.
        Lesson lesson = Lesson.create(teacherUserId, type, period, request.capacity(),
                request.notes(), !covered, zone, maxGroupCapacity);

        Lesson saved = lessons.save(lesson);
        log.info("Lesson {} scheduled", saved.id());
        return LessonView.from(saved, now, 0);
    }
}

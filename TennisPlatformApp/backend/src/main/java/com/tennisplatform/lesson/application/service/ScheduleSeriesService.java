package com.tennisplatform.lesson.application.service;

import com.tennisplatform.availability.application.port.in.QueryAvailability;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.in.ScheduleSeries;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.domain.InvalidLessonException;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonInThePastException;
import com.tennisplatform.lesson.domain.LessonOutsideAvailabilityException;
import com.tennisplatform.lesson.domain.LessonOverlapException;
import com.tennisplatform.lesson.domain.LessonPeriod;
import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.lesson.domain.WeeklySeries;
import com.tennisplatform.platform.application.port.in.GetMaxGroupCapacity;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates a weekly series with the same checks as a single lesson, date by date, and in one
 * transaction: a series is never left half made for the teacher to sort out by hand.
 */
public class ScheduleSeriesService implements ScheduleSeries {

    private static final Logger log = LoggerFactory.getLogger(ScheduleSeriesService.class);

    private final LessonRepository lessons;
    private final TeacherLessons teacher;
    private final QueryAvailability availability;
    private final GetMaxGroupCapacity groupCapacity;
    private final Clock clock;

    public ScheduleSeriesService(LessonRepository lessons, GetTeacherProfile teacherProfile,
                                 QueryAvailability availability, GetMaxGroupCapacity groupCapacity, Clock clock) {
        this.lessons = lessons;
        this.teacher = new TeacherLessons(teacherProfile);
        this.availability = availability;
        this.groupCapacity = groupCapacity;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Occurrence> preview(UUID teacherUserId, NewSeries series) {
        Template template = templateOf(teacherUserId, series);
        Instant now = clock.instant();
        return template.occurrences().stream()
                .map(period -> new Occurrence(period.startsAt(), period.endsAt(), statusOf(teacherUserId, period, now)))
                .toList();
    }

    @Override
    @Transactional
    public List<LessonView> schedule(UUID teacherUserId, NewSeries series, List<Instant> chosen,
                                     boolean overrideAvailability) {
        Template template = templateOf(teacherUserId, series);
        if (chosen == null || chosen.isEmpty()) {
            throw new InvalidLessonException("Choose at least one date of the series");
        }
        Map<Instant, LessonPeriod> byStart = template.occurrences().stream()
                .collect(Collectors.toMap(LessonPeriod::startsAt, Function.identity()));
        UUID seriesId = UUID.randomUUID();
        Instant now = clock.instant();
        List<LessonView> created = new ArrayList<>();
        for (Instant start : chosen.stream().distinct().sorted().toList()) {
            LessonPeriod period = byStart.get(start);
            if (period == null) {
                throw new InvalidLessonException(start + " is not one of the series' dates");
            }
            if (!period.startsAt().isAfter(now)) {
                throw new LessonInThePastException("A lesson has to start after the moment it is created");
            }
            boolean covered = availability.covers(teacherUserId, period.startsAt(), period.endsAt());
            if (!covered && !overrideAvailability) {
                throw new LessonOutsideAvailabilityException("The lesson of " + start
                        + " falls outside the configured availability. Change the hours or ask for it explicitly");
            }
            if (lessons.existsOverlapping(teacherUserId, period.startsAt(), period.endsAt())) {
                throw new LessonOverlapException("Another lesson already runs at " + start);
            }
            Lesson saved = lessons.save(Lesson.create(teacherUserId, template.type(), period, series.capacity(),
                    series.notes(), !covered, template.zone(), template.maxGroupCapacity(), seriesId));
            created.add(LessonView.from(saved, now, 0));
        }
        log.info("Series {} scheduled: {} lessons", seriesId, created.size());
        return created;
    }

    /**
     * The series' dates, with the first lesson checked as a lesson - type, capacity, midnight -
     * so a bad template answers 400 before any date is looked at.
     */
    private Template templateOf(UUID teacherUserId, NewSeries series) {
        ZoneId zone = teacher.requireTheTeacher(teacherUserId);
        LessonType type = LessonType.parse(series.type());
        int maxGroupCapacity = type == LessonType.GROUP ? groupCapacity.maxGroupCapacity() : 1;
        List<LessonPeriod> occurrences = WeeklySeries.occurrences(series.startsAt(), series.endsAt(), zone,
                series.weeks());
        occurrences.forEach(period -> Lesson.create(teacherUserId, type, period, series.capacity(), series.notes(),
                false, zone, maxGroupCapacity));
        return new Template(type, zone, maxGroupCapacity, occurrences);
    }

    /** A clash wins over being outside the hours: one can be overridden, the other cannot. */
    private String statusOf(UUID teacherUserId, LessonPeriod period, Instant now) {
        if (!period.startsAt().isAfter(now)) {
            return "IN_THE_PAST";
        }
        if (lessons.existsOverlapping(teacherUserId, period.startsAt(), period.endsAt())) {
            return "OVERLAP";
        }
        if (!availability.covers(teacherUserId, period.startsAt(), period.endsAt())) {
            return "OUTSIDE_AVAILABILITY";
        }
        return "OK";
    }

    private record Template(LessonType type, ZoneId zone, int maxGroupCapacity, List<LessonPeriod> occurrences) {
    }
}

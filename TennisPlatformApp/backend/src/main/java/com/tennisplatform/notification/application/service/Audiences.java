package com.tennisplatform.notification.application.service;

import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.notification.domain.AudienceNotFoundException;
import com.tennisplatform.notification.domain.InvalidMessageException;
import com.tennisplatform.notification.domain.LessonNotInSeriesException;
import com.tennisplatform.notification.domain.MessageAudience;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.QueryLevels;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Who a message reaches, and how the teacher's list of sent messages names them
 * (37-fase24-analisis-mensajes.md, decisions 1 and 2).
 *
 * <p>Always among the students the teacher manages now: a booking alone does not make somebody a
 * recipient, so a student the teacher let go stops hearing from them even if a seat was left.
 */
final class Audiences {

    private static final Locale SPANISH = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", SPANISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", SPANISH);

    private final QueryManagedStudent students;
    private final QueryLevels levels;
    private final GetLesson lessons;
    private final GetBookings bookings;
    private final GetTeacherProfile teachers;
    private final Clock clock;

    Audiences(QueryManagedStudent students, QueryLevels levels, GetLesson lessons, GetBookings bookings,
              GetTeacherProfile teachers, Clock clock) {
        this.students = students;
        this.levels = levels;
        this.lessons = lessons;
        this.bookings = bookings;
        this.teachers = teachers;
        this.clock = clock;
    }

    record Resolved(String label, Set<UUID> recipients) {

        Resolved {
            recipients = Set.copyOf(recipients);
        }
    }

    Resolved resolve(UUID teacherUserId, MessageAudience audience, UUID levelId, UUID lessonId) {
        if ((levelId != null) != (audience == MessageAudience.LEVEL)) {
            throw new InvalidMessageException("A level goes with the LEVEL audience and only with it");
        }
        boolean ofALesson = audience == MessageAudience.LESSON || audience == MessageAudience.SERIES;
        if ((lessonId != null) != ofALesson) {
            throw new InvalidMessageException("A lesson goes with the LESSON and SERIES audiences and only with them");
        }
        return switch (audience) {
            case ALL -> new Resolved("Todos los alumnos", Set.copyOf(students.studentsOf(teacherUserId)));
            case LEVEL -> level(teacherUserId, levelId);
            case LESSON -> lesson(teacherUserId, lessonId);
            case SERIES -> series(teacherUserId, lessonId);
        };
    }

    private Resolved level(UUID teacherUserId, UUID levelId) {
        if (!levels.belongsTo(levelId, teacherUserId)) {
            throw AudienceNotFoundException.level(levelId);
        }
        return new Resolved("Nivel " + levels.nameOf(levelId), Set.copyOf(students.studentsOn(teacherUserId, levelId)));
    }

    private Resolved lesson(UUID teacherUserId, UUID lessonId) {
        LessonView lesson = owned(teacherUserId, lessonId);
        String label = "Clase del " + DAY.format(lesson.startsAt().atZone(zoneOf(teacherUserId)));
        return new Resolved(label, managedAmong(teacherUserId, bookings.studentsWithASeatIn(List.of(lessonId))));
    }

    /** Whoever has a seat in a lesson of the series that has not started: a past seat is not a place in the group. */
    private Resolved series(UUID teacherUserId, UUID lessonId) {
        LessonView lesson = owned(teacherUserId, lessonId);
        if (lesson.seriesId() == null) {
            throw new LessonNotInSeriesException("The lesson is not part of a weekly series");
        }
        Instant now = clock.instant();
        List<UUID> upcoming = lessons.ofSeries(lesson.seriesId()).stream()
                .filter(each -> each.startsAt().isAfter(now))
                .map(LessonView::id)
                .toList();
        ZonedDateTime start = lesson.startsAt().atZone(zoneOf(teacherUserId));
        String label = "Grupo de los " + plural(start.getDayOfWeek()) + " a las " + TIME.format(start);
        return new Resolved(label, managedAmong(teacherUserId, bookings.studentsWithASeatIn(upcoming)));
    }

    /** Another teacher's lesson answers like one that does not exist. */
    private LessonView owned(UUID teacherUserId, UUID lessonId) {
        LessonView lesson = lessons.byId(lessonId);
        if (!lesson.teacherUserId().equals(teacherUserId)) {
            throw AudienceNotFoundException.lesson(lessonId);
        }
        return lesson;
    }

    private Set<UUID> managedAmong(UUID teacherUserId, Set<UUID> booked) {
        Set<UUID> managed = new LinkedHashSet<>(students.studentsOf(teacherUserId));
        managed.retainAll(booked);
        return managed;
    }

    private ZoneId zoneOf(UUID teacherUserId) {
        return ZoneId.of(teachers.byUserId(teacherUserId).orElseThrow().timezone());
    }

    /** "los lunes", "los sábados": the days that do not already end in s take one. */
    private static String plural(DayOfWeek day) {
        String name = day.getDisplayName(TextStyle.FULL, SPANISH);
        return name.endsWith("s") ? name : name + "s";
    }
}

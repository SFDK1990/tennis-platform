package com.tennisplatform.lesson.application.service;

import com.tennisplatform.lesson.application.port.in.EditLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.application.port.spi.LessonBookings;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonNotFoundException;
import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.platform.application.port.in.GetMaxGroupCapacity;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class EditLessonService implements EditLesson {

    private static final Logger log = LoggerFactory.getLogger(EditLessonService.class);

    private final LessonRepository lessons;
    private final LessonBookings bookings;
    private final GetMaxGroupCapacity groupCapacity;
    private final TeacherLessons teacher;
    private final Clock clock;

    public EditLessonService(LessonRepository lessons, LessonBookings bookings, GetMaxGroupCapacity groupCapacity,
                             GetTeacherProfile teacherProfile, Clock clock) {
        this.lessons = lessons;
        this.bookings = bookings;
        this.groupCapacity = groupCapacity;
        this.teacher = new TeacherLessons(teacherProfile);
        this.clock = clock;
    }

    /**
     * Locks the lesson the way a booking does before counting its seats. Without the lock a
     * booking could get in between the count and the write, and lowering the capacity would leave
     * more students than seats.
     */
    @Override
    @Transactional
    public LessonView edit(UUID teacherUserId, UUID lessonId, Changes changes) {
        teacher.requireTheTeacher(teacherUserId);
        Lesson lesson = lessons.findByIdForUpdate(lessonId)
                .filter(found -> found.teacherUserId().equals(teacherUserId))
                .orElseThrow(() -> new LessonNotFoundException("No lesson with id " + lessonId));

        int booked = bookings.countConfirmed(List.of(lessonId)).getOrDefault(lessonId, 0);
        int maxGroupCapacity = lesson.type() == LessonType.GROUP ? groupCapacity.maxGroupCapacity() : 1;
        Instant now = clock.instant();
        Lesson edited = lessons.save(lesson.edit(changes.notes(), changes.capacity(), booked, now, maxGroupCapacity));
        log.info("Lesson {} edited", lessonId);
        return LessonView.from(edited, now, booked);
    }
}

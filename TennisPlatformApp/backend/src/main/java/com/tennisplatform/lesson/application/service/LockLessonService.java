package com.tennisplatform.lesson.application.service;

import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.in.LockLesson;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.application.port.spi.LessonBookings;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonNotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class LockLessonService implements LockLesson {

    private final LessonRepository lessons;
    private final LessonBookings bookings;
    private final Clock clock;

    public LockLessonService(LessonRepository lessons, LessonBookings bookings, Clock clock) {
        this.lessons = lessons;
        this.bookings = bookings;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public LessonView lockForBooking(UUID lessonId) {
        Lesson lesson = lessons.findByIdForUpdate(lessonId)
                .orElseThrow(() -> new LessonNotFoundException("No lesson with id " + lessonId));
        int booked = bookings.countConfirmed(List.of(lessonId)).getOrDefault(lessonId, 0);
        return LessonView.from(lesson, clock.instant(), booked);
    }
}

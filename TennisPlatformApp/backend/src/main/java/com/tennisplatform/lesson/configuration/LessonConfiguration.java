package com.tennisplatform.lesson.configuration;

import com.tennisplatform.availability.application.port.in.QueryAvailability;
import com.tennisplatform.lesson.application.port.in.CancelLesson;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.ScheduleLesson;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.application.service.CancelLessonService;
import com.tennisplatform.lesson.application.service.GetLessonService;
import com.tennisplatform.lesson.application.service.ScheduleLessonService;
import com.tennisplatform.platform.application.port.in.GetMaxGroupCapacity;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the use cases by hand, like the other modules do, so the application layer stays free of
 * Spring stereotypes.
 */
@Configuration
public class LessonConfiguration {

    @Bean
    public ScheduleLesson scheduleLesson(LessonRepository lessons, GetTeacherProfile teacherProfile,
                                         QueryAvailability availability,
                                         GetMaxGroupCapacity groupCapacity, Clock clock) {
        return new ScheduleLessonService(lessons, teacherProfile, availability, groupCapacity, clock);
    }

    @Bean
    public GetLesson getLesson(LessonRepository lessons, GetTeacherProfile teacherProfile, Clock clock) {
        return new GetLessonService(lessons, teacherProfile, clock);
    }

    @Bean
    public CancelLesson cancelLesson(LessonRepository lessons, GetTeacherProfile teacherProfile,
                                     Clock clock) {
        return new CancelLessonService(lessons, teacherProfile, clock);
    }
}

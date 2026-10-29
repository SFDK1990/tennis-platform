package com.tennisplatform.lesson.configuration;

import com.tennisplatform.availability.application.port.in.QueryAvailability;
import com.tennisplatform.lesson.application.port.in.CancelLesson;
import com.tennisplatform.lesson.application.port.in.EditLesson;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LockLesson;
import com.tennisplatform.lesson.application.port.in.ScheduleLesson;
import com.tennisplatform.lesson.application.port.in.ScheduleSeries;
import com.tennisplatform.lesson.application.port.in.Courts;
import com.tennisplatform.lesson.application.port.out.CourtRepository;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.application.service.CourtsService;
import com.tennisplatform.lesson.application.port.spi.LessonBookings;
import com.tennisplatform.lesson.application.service.CancelLessonService;
import com.tennisplatform.lesson.application.service.EditLessonService;
import com.tennisplatform.lesson.application.service.GetLessonService;
import com.tennisplatform.lesson.application.service.LockLessonService;
import com.tennisplatform.lesson.application.service.ScheduleLessonService;
import com.tennisplatform.lesson.application.service.ScheduleSeriesService;
import com.tennisplatform.platform.application.port.in.GetMaxGroupCapacity;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import org.springframework.context.ApplicationEventPublisher;
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
                                         GetMaxGroupCapacity groupCapacity, QueryLevels levels, CourtRepository courts,
                                         Clock clock) {
        return new ScheduleLessonService(lessons, teacherProfile, availability, groupCapacity, levels, courts, clock);
    }

    @Bean
    public ScheduleSeries scheduleSeries(LessonRepository lessons, GetTeacherProfile teacherProfile,
                                         QueryAvailability availability,
                                         GetMaxGroupCapacity groupCapacity, QueryLevels levels, CourtRepository courts,
                                         Clock clock) {
        return new ScheduleSeriesService(lessons, teacherProfile, availability, groupCapacity, levels, courts, clock);
    }

    @Bean
    public GetLesson getLesson(LessonRepository lessons, LessonBookings bookings,
                               GetTeacherProfile teacherProfile, Clock clock) {
        return new GetLessonService(lessons, bookings, teacherProfile, clock);
    }

    @Bean
    public EditLesson editLesson(LessonRepository lessons, LessonBookings bookings,
                                 GetMaxGroupCapacity groupCapacity, GetTeacherProfile teacherProfile,
                                 QueryLevels levels, CourtRepository courts, ApplicationEventPublisher events,
                                 Clock clock) {
        return new EditLessonService(lessons, bookings, groupCapacity, teacherProfile, levels, courts, events, clock);
    }

    @Bean
    public Courts courts(CourtRepository courts, GetTeacherProfile teacherProfile) {
        return new CourtsService(courts, teacherProfile);
    }

    @Bean
    public CancelLesson cancelLesson(LessonRepository lessons, LessonBookings bookings,
                                     GetTeacherProfile teacherProfile, Clock clock) {
        return new CancelLessonService(lessons, bookings, teacherProfile, clock);
    }

    /**
     * {@link LessonBookings} is not declared anywhere in this module: {@code booking} provides
     * it. If that module ever stops doing so, the context refuses to start, which is the point -
     * a cascade that silently stopped happening would be far worse.
     */
    @Bean
    public LockLesson lockLesson(LessonRepository lessons, LessonBookings bookings, Clock clock) {
        return new LockLessonService(lessons, bookings, clock);
    }
}

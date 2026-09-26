package com.tennisplatform.calendar.configuration;

import com.tennisplatform.availability.application.port.in.QueryAvailability;
import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.calendar.application.port.in.GetCalendar;
import com.tennisplatform.calendar.application.service.GetCalendarService;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CalendarConfiguration {

    @Bean
    public GetCalendar getCalendar(GetTeacherProfile teacherProfiles, QueryAvailability availability,
                                   GetLesson lessons, QueryManagedStudent managedStudents,
                                   GetBookings bookings) {
        return new GetCalendarService(teacherProfiles, availability, lessons, managedStudents, bookings);
    }
}

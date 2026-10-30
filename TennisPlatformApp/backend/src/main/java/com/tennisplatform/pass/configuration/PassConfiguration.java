package com.tennisplatform.pass.configuration;

import com.tennisplatform.pass.application.port.out.PassRepository;
import com.tennisplatform.pass.application.port.out.PassSettingsRepository;
import com.tennisplatform.pass.application.port.out.PassTypeRepository;
import com.tennisplatform.pass.application.port.spi.PassBookings;
import com.tennisplatform.pass.application.service.PassTypesService;
import com.tennisplatform.pass.application.service.PassesService;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the use cases by hand, like the other modules do. {@code PassBookings} is what
 * {@code booking} provides.
 */
@Configuration
public class PassConfiguration {

    @Bean
    public PassTypesService passTypesService(PassTypeRepository types, PassSettingsRepository settings,
                                             GetTeacherProfile teacherProfile) {
        return new PassTypesService(types, settings, teacherProfile);
    }

    @Bean
    public PassesService passesService(PassRepository passes, PassTypeRepository types,
                                       PassSettingsRepository settings, PassBookings bookings,
                                       QueryManagedStudent managedStudents, GetTeacherProfile teacherProfile,
                                       Clock clock) {
        return new PassesService(passes, types, settings, bookings, managedStudents, teacherProfile, clock);
    }
}

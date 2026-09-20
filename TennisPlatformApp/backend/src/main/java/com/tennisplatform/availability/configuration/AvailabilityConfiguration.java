package com.tennisplatform.availability.configuration;

import com.tennisplatform.availability.application.port.in.ConfigureWeeklyAvailability;
import com.tennisplatform.availability.application.port.in.GetAvailability;
import com.tennisplatform.availability.application.port.in.ManageAvailabilityExceptions;
import com.tennisplatform.availability.application.port.in.QueryAvailability;
import com.tennisplatform.availability.application.port.out.AvailabilityOverrideRepository;
import com.tennisplatform.availability.application.port.out.AvailabilityRuleRepository;
import com.tennisplatform.availability.application.service.ConfigureWeeklyAvailabilityService;
import com.tennisplatform.availability.application.service.GetAvailabilityService;
import com.tennisplatform.availability.application.service.ManageAvailabilityExceptionsService;
import com.tennisplatform.availability.application.service.QueryAvailabilityService;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the use cases by hand, like the other modules do, so the application layer stays free of
 * Spring stereotypes.
 */
@Configuration
public class AvailabilityConfiguration {

    @Bean
    public GetAvailability getAvailability(AvailabilityRuleRepository rules,
                                           AvailabilityOverrideRepository exceptions,
                                           GetTeacherProfile teacherProfile) {
        return new GetAvailabilityService(rules, exceptions, teacherProfile);
    }

    @Bean
    public QueryAvailability queryAvailability(AvailabilityRuleRepository rules,
                                               AvailabilityOverrideRepository exceptions,
                                               GetTeacherProfile teacherProfile) {
        return new QueryAvailabilityService(rules, exceptions, teacherProfile);
    }

    @Bean
    public ConfigureWeeklyAvailability configureWeeklyAvailability(AvailabilityRuleRepository rules,
                                                                   GetTeacherProfile teacherProfile) {
        return new ConfigureWeeklyAvailabilityService(rules, teacherProfile);
    }

    @Bean
    public ManageAvailabilityExceptions manageAvailabilityExceptions(
            AvailabilityOverrideRepository exceptions, GetTeacherProfile teacherProfile) {
        return new ManageAvailabilityExceptionsService(exceptions, teacherProfile);
    }
}

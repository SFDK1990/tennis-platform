package com.tennisplatform.pass.configuration;

import com.tennisplatform.pass.application.port.out.PassPurchaseRepository;
import com.tennisplatform.pass.application.port.out.PassRepository;
import com.tennisplatform.pass.application.port.out.PassSettingsRepository;
import com.tennisplatform.pass.application.port.out.PassTypeRepository;
import com.tennisplatform.pass.application.port.out.PaymentGateway;
import com.tennisplatform.pass.application.port.spi.PassBookings;
import com.tennisplatform.pass.application.service.PassPurchasesService;
import com.tennisplatform.pass.application.service.PassTypesService;
import com.tennisplatform.pass.application.service.PassesService;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the use cases by hand, like the other modules do. {@code PassBookings} is what
 * {@code booking} provides; {@code PaymentGateway}, the Stripe adapter.
 */
@Configuration
@EnableConfigurationProperties(PassProperties.class)
public class PassConfiguration {

    @Bean
    public PassTypesService passTypesService(PassTypeRepository types, PassSettingsRepository settings,
                                             GetTeacherProfile teacherProfile, PaymentGateway gateway) {
        return new PassTypesService(types, settings, teacherProfile, gateway);
    }

    @Bean
    public PassesService passesService(PassRepository passes, PassTypeRepository types,
                                       PassSettingsRepository settings, PassBookings bookings,
                                       QueryManagedStudent managedStudents, GetTeacherProfile teacherProfile,
                                       Clock clock) {
        return new PassesService(passes, types, settings, bookings, managedStudents, teacherProfile, clock);
    }

    @Bean
    public PassPurchasesService passPurchasesService(PassPurchaseRepository purchases, PassRepository passes,
                                                     PassTypeRepository types, PassSettingsRepository settings,
                                                     PaymentGateway gateway, QueryManagedStudent managedStudents,
                                                     GetTeacherProfile teacherProfile, Clock clock) {
        return new PassPurchasesService(purchases, passes, types, settings, gateway, managedStudents, teacherProfile,
                clock);
    }
}

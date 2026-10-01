package com.tennisplatform.student.configuration;

import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.platform.application.port.in.GetStudentLimit;
import com.tennisplatform.student.application.port.in.ManageStudent;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.application.port.out.StudentProfileRepository;
import com.tennisplatform.student.application.port.spi.StudentBookings;
import com.tennisplatform.student.application.service.GetManagedStudentsService;
import com.tennisplatform.student.application.service.ManageStudentService;
import com.tennisplatform.student.application.service.StudentProfileService;
import com.tennisplatform.identity.application.port.in.CloseAccount;
import com.tennisplatform.student.application.port.in.DeleteMyAccount;
import com.tennisplatform.student.application.service.DeleteMyAccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the use cases by hand, like the other modules do, so the application layer stays free
 * of Spring stereotypes.
 */
@Configuration
public class StudentConfiguration {

    /**
     * Declared by its concrete type because it implements both profile ports and the two are
     * one use case seen from two sides. Spring injects it wherever either port is asked for;
     * two beans would mean two instances of the same thing.
     */
    @Bean
    public StudentProfileService studentProfileService(StudentProfileRepository profiles,
                                                       Clock clock) {
        return new StudentProfileService(profiles, clock);
    }

    @Bean
    public ManageStudent manageStudent(ManagedStudentRepository relationships,
                                       StudentProfileRepository profiles,
                                       FindUserAccounts accounts, GetStudentLimit studentLimit,
                                       StudentBookings bookings, Clock clock) {
        return new ManageStudentService(relationships, profiles, accounts, studentLimit, bookings,
                clock);
    }

    /**
     * Declared by its concrete type, like {@link #studentProfileService}: it implements both
     * {@code GetManagedStudents} and {@code QueryManagedStudent}, and one bean serves both.
     */
    @Bean
    public DeleteMyAccount deleteMyAccount(CloseAccount accounts, ManagedStudentRepository relationships,
                                           StudentProfileRepository profiles, StudentBookings bookings,
                                           Clock clock) {
        return new DeleteMyAccountService(accounts, relationships, profiles, bookings, clock);
    }

    @Bean
    public GetManagedStudentsService getManagedStudents(ManagedStudentRepository relationships,
                                                        StudentProfileRepository profiles,
                                                        FindUserAccounts accounts) {
        return new GetManagedStudentsService(relationships, profiles, accounts);
    }
}

package com.tennisplatform.student.configuration;

import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.platform.application.port.in.GetStudentLimit;
import com.tennisplatform.student.application.port.in.GetManagedStudents;
import com.tennisplatform.student.application.port.in.ManageStudent;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.application.port.out.StudentProfileRepository;
import com.tennisplatform.student.application.service.GetManagedStudentsService;
import com.tennisplatform.student.application.service.ManageStudentService;
import com.tennisplatform.student.application.service.StudentProfileService;
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
                                       Clock clock) {
        return new ManageStudentService(relationships, profiles, accounts, studentLimit, clock);
    }

    @Bean
    public GetManagedStudents getManagedStudents(ManagedStudentRepository relationships,
                                                 StudentProfileRepository profiles,
                                                 FindUserAccounts accounts) {
        return new GetManagedStudentsService(relationships, profiles, accounts);
    }
}

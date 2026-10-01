package com.tennisplatform.student.application.service;

import com.tennisplatform.identity.application.port.in.CloseAccount;
import com.tennisplatform.student.application.port.in.DeleteMyAccount;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.application.port.out.StudentProfileRepository;
import com.tennisplatform.student.application.port.spi.StudentBookings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Lives here because this is the module that may reach both halves: the account through
 * identity's inbound port, and the bookings through the SPI that {@code booking} implements.
 */
public class DeleteMyAccountService implements DeleteMyAccount {

    private static final Logger log = LoggerFactory.getLogger(DeleteMyAccountService.class);

    private final CloseAccount accounts;
    private final ManagedStudentRepository relationships;
    private final StudentProfileRepository profiles;
    private final StudentBookings bookings;
    private final Clock clock;

    public DeleteMyAccountService(CloseAccount accounts, ManagedStudentRepository relationships,
                                  StudentProfileRepository profiles, StudentBookings bookings, Clock clock) {
        this.accounts = accounts;
        this.relationships = relationships;
        this.profiles = profiles;
        this.bookings = bookings;
        this.clock = clock;
    }

    /** The account goes first because it checks the password: a wrong one changes nothing. */
    @Override
    @Transactional
    public void delete(UUID studentUserId, String password) {
        accounts.close(studentUserId, password);

        Instant now = clock.instant();
        for (UUID teacherUserId : relationships.findManagingTeachers(studentUserId)) {
            relationships.findByPair(teacherUserId, studentUserId).ifPresent(relationship -> {
                relationship.deactivate(now);
                relationships.save(relationship);
            });
            bookings.cancelUpcomingOfDeletedAccount(teacherUserId, studentUserId, now);
        }
        profiles.findByUserId(studentUserId).ifPresent(profile -> {
            profile.anonymize();
            profiles.save(profile);
        });
        log.info("Account {} deleted by its student, upcoming bookings cancelled and profile emptied", studentUserId);
    }
}

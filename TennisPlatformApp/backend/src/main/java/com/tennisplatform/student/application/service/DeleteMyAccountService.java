package com.tennisplatform.student.application.service;

import com.tennisplatform.identity.application.port.in.CloseAccount;
import com.tennisplatform.student.application.port.in.DeleteMyAccount;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.application.port.out.StudentProfileRepository;
import com.tennisplatform.student.application.port.spi.StudentBookings;
import com.tennisplatform.student.application.port.spi.StudentMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Lives here because this is the module that may reach both halves: the account through
 * identity's inbound port, and the bookings and the messages through the SPIs that {@code booking}
 * and {@code notification} implement.
 */
public class DeleteMyAccountService implements DeleteMyAccount {

    private static final Logger log = LoggerFactory.getLogger(DeleteMyAccountService.class);

    private final CloseAccount accounts;
    private final ManagedStudentRepository relationships;
    private final StudentProfileRepository profiles;
    private final StudentBookings bookings;
    private final StudentMessages messages;
    private final Clock clock;

    public DeleteMyAccountService(CloseAccount accounts, ManagedStudentRepository relationships,
                                  StudentProfileRepository profiles, StudentBookings bookings,
                                  StudentMessages messages, Clock clock) {
        this.accounts = accounts;
        this.relationships = relationships;
        this.profiles = profiles;
        this.bookings = bookings;
        this.messages = messages;
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
        for (var relationship : relationships.findAllByStudent(studentUserId)) {
            relationship.clearRecord();
            relationships.save(relationship);
        }
        profiles.findByUserId(studentUserId).ifPresent(profile -> {
            profile.anonymize();
            profiles.save(profile);
        });
        messages.forgetRecipient(studentUserId);
        log.info("Account {} deleted by its student, upcoming bookings cancelled, profile emptied and messages forgotten",
                studentUserId);
    }
}

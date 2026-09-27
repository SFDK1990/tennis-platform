package com.tennisplatform.student.application.service;

import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.platform.application.port.in.GetStudentLimit;
import com.tennisplatform.student.application.port.in.ManageStudent;
import com.tennisplatform.student.application.port.in.ManagedStudentView;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.application.port.out.StudentProfileRepository;
import com.tennisplatform.student.application.port.spi.StudentBookings;
import com.tennisplatform.student.domain.ManagedStudent;
import com.tennisplatform.student.domain.StudentAlreadyManagedException;
import com.tennisplatform.student.domain.StudentLimitReachedException;
import com.tennisplatform.student.domain.StudentNotManagedException;
import com.tennisplatform.student.domain.StudentProfileIncompleteException;
import com.tennisplatform.student.domain.StudentProfileNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Taking a student under management and letting them go.
 *
 * <p>Both run inside one transaction that reads the limit, counts the current relationships and
 * writes: checking the limit outside the transaction would let two concurrent calls both see
 * room for one more. The MVP has a single teacher clicking a single button, so the race is
 * unlikely - but the shape of the code is what survives into the phases where it is not.
 */
public class ManageStudentService implements ManageStudent {

    private final ManagedStudentRepository relationships;
    private final StudentProfileRepository profiles;
    private final FindUserAccounts accounts;
    private final GetStudentLimit studentLimit;
    private final StudentBookings bookings;
    private final Clock clock;

    public ManageStudentService(ManagedStudentRepository relationships,
                                StudentProfileRepository profiles, FindUserAccounts accounts,
                                GetStudentLimit studentLimit, StudentBookings bookings,
                                Clock clock) {
        this.relationships = relationships;
        this.profiles = profiles;
        this.accounts = accounts;
        this.studentLimit = studentLimit;
        this.bookings = bookings;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ManagedStudentView manage(UUID teacherUserId, UUID studentUserId) {
        UserSummary account = studentAccount(studentUserId);
        String fullName = profiles.findByUserId(studentUserId)
                .orElseThrow(() -> new StudentProfileIncompleteException(
                        "This student has not filled their personal data in yet"))
                .fullName();

        ManagedStudent relationship = relationships.findByPair(teacherUserId, studentUserId)
                .map(this::takeBack)
                .orElseGet(() -> {
                    refuseIfLimitReached(teacherUserId);
                    return ManagedStudent.take(teacherUserId, studentUserId, clock.instant());
                });

        ManagedStudent saved = relationships.save(relationship);
        return new ManagedStudentView(studentUserId, account.email(), fullName,
                saved.status().name(), saved.managedAt());
    }

    @Override
    @Transactional
    public void stopManaging(UUID teacherUserId, UUID studentUserId) {
        ManagedStudent relationship = relationships.findByPair(teacherUserId, studentUserId)
                .orElseThrow(() -> new StudentNotManagedException(
                        "This teacher has never managed this student"));

        Instant now = clock.instant();
        relationship.deactivate(now);
        relationships.save(relationship);
        bookings.cancelUpcomingWith(teacherUserId, studentUserId, now);
    }

    @Override
    @Transactional
    public void releaseDisabledAccount(UUID studentUserId) {
        Instant now = clock.instant();
        for (UUID teacherUserId : relationships.findManagingTeachers(studentUserId)) {
            relationships.findByPair(teacherUserId, studentUserId).ifPresent(relationship -> {
                relationship.deactivate(now);
                relationships.save(relationship);
            });
            bookings.cancelUpcomingOfDisabledAccount(teacherUserId, studentUserId, now);
        }
    }

    /**
     * Reactivates the existing row instead of creating a second one. The limit applies here
     * too: taking somebody back costs a slot exactly like taking somebody on.
     */
    private ManagedStudent takeBack(ManagedStudent relationship) {
        if (relationship.isManaged()) {
            throw new StudentAlreadyManagedException("This student is already managed");
        }
        refuseIfLimitReached(relationship.teacherUserId());
        relationship.reactivate(clock.instant());
        return relationship;
    }

    private void refuseIfLimitReached(UUID teacherUserId) {
        int limit = studentLimit.studentLimit();
        if (relationships.countManagedBy(teacherUserId) >= limit) {
            throw new StudentLimitReachedException(
                    "The platform allows managing at most " + limit + " students at a time");
        }
    }

    /**
     * The id has to belong to an account with the STUDENT role. Comparing the role as text is
     * deliberate: importing identity's {@code Role} enum here would reach into another module's
     * domain, which the boundary rules reject.
     */
    private UserSummary studentAccount(UUID studentUserId) {
        UserSummary account = accounts.byIds(List.of(studentUserId))
                .get(studentUserId);
        if (account == null || !"STUDENT".equals(account.role())) {
            throw new StudentProfileNotFoundException("There is no student account with this id");
        }
        return account;
    }
}

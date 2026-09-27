package com.tennisplatform.student.application;

import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.platform.application.port.in.GetStudentLimit;
import com.tennisplatform.student.application.port.in.ManagedStudentView;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.application.port.out.StudentProfileRepository;
import com.tennisplatform.student.application.port.spi.StudentBookings;
import com.tennisplatform.student.application.service.ManageStudentService;
import com.tennisplatform.student.domain.ManagedStatus;
import com.tennisplatform.student.domain.ManagedStudent;
import com.tennisplatform.student.domain.StudentAlreadyManagedException;
import com.tennisplatform.student.domain.StudentLimitReachedException;
import com.tennisplatform.student.domain.StudentNotManagedException;
import com.tennisplatform.student.domain.StudentProfile;
import com.tennisplatform.student.domain.StudentProfileIncompleteException;
import com.tennisplatform.student.domain.StudentProfileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManageStudentServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    private ManagedStudentRepository relationships;
    private StudentProfileRepository profiles;
    private FindUserAccounts accounts;
    private GetStudentLimit studentLimit;
    private StudentBookings bookings;
    private ManageStudentService service;

    @BeforeEach
    void setUp() {
        relationships = mock(ManagedStudentRepository.class);
        profiles = mock(StudentProfileRepository.class);
        accounts = mock(FindUserAccounts.class);
        studentLimit = mock(GetStudentLimit.class);
        bookings = mock(StudentBookings.class);
        service = new ManageStudentService(relationships, profiles, accounts, studentLimit, bookings,
                Clock.fixed(NOW, ZoneOffset.UTC));

        when(accounts.byIds(List.of(STUDENT_ID))).thenReturn(Map.of(STUDENT_ID,
                new UserSummary(STUDENT_ID, "ana@example.com", "STUDENT", "ACTIVE", NOW)));
        when(profiles.findByUserId(STUDENT_ID)).thenReturn(Optional.of(
                StudentProfile.create(STUDENT_ID, "Ana Ruiz", null, null, null, NOW)));
        when(relationships.findByPair(TEACHER_ID, STUDENT_ID)).thenReturn(Optional.empty());
        when(relationships.countManagedBy(TEACHER_ID)).thenReturn(0L);
        when(studentLimit.studentLimit()).thenReturn(50);
        when(relationships.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void managingAStudentCreatesTheRelationship() {
        ManagedStudentView view = service.manage(TEACHER_ID, STUDENT_ID);

        assertThat(view.userId()).isEqualTo(STUDENT_ID);
        assertThat(view.email()).isEqualTo("ana@example.com");
        assertThat(view.fullName()).isEqualTo("Ana Ruiz");
        assertThat(view.managedStatus()).isEqualTo("MANAGED");
    }

    /** Criterion 3 of Fase 6: managing twice is refused and no second row is written. */
    @Test
    void managingTheSameStudentTwiceIsRefused() {
        when(relationships.findByPair(TEACHER_ID, STUDENT_ID))
                .thenReturn(Optional.of(ManagedStudent.take(TEACHER_ID, STUDENT_ID, NOW)));

        assertThatThrownBy(() -> service.manage(TEACHER_ID, STUDENT_ID))
                .isInstanceOf(StudentAlreadyManagedException.class);
        verify(relationships, never()).save(any());
    }

    /**
     * Criterion 7 of Fase 6: taking a deactivated student back updates the existing row rather
     * than adding a second relationship, which is what the unique index would otherwise have to
     * catch as a constraint violation.
     */
    @Test
    void takingBackADeactivatedStudentReusesTheSameRow() {
        ManagedStudent existing = ManagedStudent.take(TEACHER_ID, STUDENT_ID, NOW);
        existing.deactivate(NOW);
        when(relationships.findByPair(TEACHER_ID, STUDENT_ID)).thenReturn(Optional.of(existing));

        service.manage(TEACHER_ID, STUDENT_ID);

        ArgumentCaptor<ManagedStudent> saved = ArgumentCaptor.forClass(ManagedStudent.class);
        verify(relationships).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(existing.id());
        assertThat(saved.getValue().status()).isEqualTo(ManagedStatus.MANAGED);
    }

    @Test
    void theLimitFromThePlatformConfigurationIsEnforced() {
        when(studentLimit.studentLimit()).thenReturn(2);
        when(relationships.countManagedBy(TEACHER_ID)).thenReturn(2L);

        assertThatThrownBy(() -> service.manage(TEACHER_ID, STUDENT_ID))
                .isInstanceOf(StudentLimitReachedException.class)
                .hasMessageContaining("2");
        verify(relationships, never()).save(any());
    }

    /** Taking somebody back costs a slot exactly like taking somebody on for the first time. */
    @Test
    void theLimitAlsoAppliesWhenTakingAStudentBack() {
        ManagedStudent existing = ManagedStudent.take(TEACHER_ID, STUDENT_ID, NOW);
        existing.deactivate(NOW);
        when(relationships.findByPair(TEACHER_ID, STUDENT_ID)).thenReturn(Optional.of(existing));
        when(studentLimit.studentLimit()).thenReturn(1);
        when(relationships.countManagedBy(TEACHER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.manage(TEACHER_ID, STUDENT_ID))
                .isInstanceOf(StudentLimitReachedException.class);
    }

    @Test
    void aStudentWhoHasNotFilledTheirDataInCannotBeManagedYet() {
        when(profiles.findByUserId(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.manage(TEACHER_ID, STUDENT_ID))
                .isInstanceOf(StudentProfileIncompleteException.class);
    }

    /** An id that belongs to the teacher, or to nobody, is not a student to manage. */
    @Test
    void onlyAStudentAccountCanBeManaged() {
        when(accounts.byIds(List.of(STUDENT_ID))).thenReturn(Map.of(STUDENT_ID,
                new UserSummary(STUDENT_ID, "teacher@example.com", "TEACHER", "ACTIVE", NOW)));

        assertThatThrownBy(() -> service.manage(TEACHER_ID, STUDENT_ID))
                .isInstanceOf(StudentProfileNotFoundException.class);
    }

    @Test
    void stoppingToManageSomebodyNeverManagedIsRefused() {
        assertThatThrownBy(() -> service.stopManaging(TEACHER_ID, STUDENT_ID))
                .isInstanceOf(StudentNotManagedException.class);

        verify(bookings, never()).cancelUpcomingWith(any(), any(), any());
    }

    /** Criterion of Fase 9: letting a student go cancels their upcoming bookings with this teacher. */
    @Test
    void stoppingToManageCancelsTheStudentsUpcomingBookingsWithThisTeacher() {
        when(relationships.findByPair(TEACHER_ID, STUDENT_ID))
                .thenReturn(Optional.of(ManagedStudent.take(TEACHER_ID, STUDENT_ID, NOW)));

        service.stopManaging(TEACHER_ID, STUDENT_ID);

        verify(bookings).cancelUpcomingWith(TEACHER_ID, STUDENT_ID, NOW);
    }

    /** Fase 12: a disabled account is let go by every teacher, and the bookings say who decided. */
    @Test
    void aDisabledAccountIsLetGoWithItsBookingsCancelledByTheAdministration() {
        ManagedStudent existing = ManagedStudent.take(TEACHER_ID, STUDENT_ID, NOW);
        when(relationships.findManagingTeachers(STUDENT_ID)).thenReturn(List.of(TEACHER_ID));
        when(relationships.findByPair(TEACHER_ID, STUDENT_ID)).thenReturn(Optional.of(existing));

        service.releaseDisabledAccount(STUDENT_ID);

        assertThat(existing.isManaged()).isFalse();
        verify(bookings).cancelUpcomingOfDisabledAccount(TEACHER_ID, STUDENT_ID, NOW);
        verify(bookings, never()).cancelUpcomingWith(any(), any(), any());
    }

    @Test
    void stoppingToManageDeactivatesWithoutDeletingTheRow() {
        ManagedStudent existing = ManagedStudent.take(TEACHER_ID, STUDENT_ID, NOW);
        when(relationships.findByPair(TEACHER_ID, STUDENT_ID)).thenReturn(Optional.of(existing));

        service.stopManaging(TEACHER_ID, STUDENT_ID);

        ArgumentCaptor<ManagedStudent> saved = ArgumentCaptor.forClass(ManagedStudent.class);
        verify(relationships).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(ManagedStatus.INACTIVE);
        assertThat(saved.getValue().deactivatedAt()).isEqualTo(NOW);
    }
}

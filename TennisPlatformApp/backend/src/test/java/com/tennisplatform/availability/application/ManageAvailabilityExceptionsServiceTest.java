package com.tennisplatform.availability.application;

import com.tennisplatform.availability.application.port.in.NewAvailabilityOverride;
import com.tennisplatform.availability.application.port.out.AvailabilityOverrideRepository;
import com.tennisplatform.availability.application.service.ManageAvailabilityExceptionsService;
import com.tennisplatform.availability.domain.AvailabilityOverrideNotFoundException;
import com.tennisplatform.availability.domain.InvalidAvailabilityException;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManageAvailabilityExceptionsServiceTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 5, 4);

    private AvailabilityOverrideRepository exceptions;
    private GetTeacherProfile teacherProfile;
    private ManageAvailabilityExceptionsService service;

    @BeforeEach
    void setUp() {
        exceptions = mock(AvailabilityOverrideRepository.class);
        teacherProfile = mock(GetTeacherProfile.class);
        when(teacherProfile.byUserId(TEACHER_ID)).thenReturn(Optional.of(
                new TeacherProfileView(TEACHER_ID, "The teacher", null, "Europe/Madrid")));

        service = new ManageAvailabilityExceptionsService(exceptions, teacherProfile);
    }

    @Test
    void removingSomethingThatIsNotThereIsANotFound() {
        when(exceptions.deleteByIdAndTeacher(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.remove(TEACHER_ID, UUID.randomUUID()))
                .isInstanceOf(AvailabilityOverrideNotFoundException.class);
    }

    /**
     * The delete filters by teacher, so an id owned by somebody else comes back as "not found"
     * rather than as a 403 that would confirm the id exists.
     */
    @Test
    void theDeleteIsScopedToTheCallingTeacher() {
        UUID exceptionId = UUID.randomUUID();
        when(exceptions.deleteByIdAndTeacher(exceptionId, TEACHER_ID)).thenReturn(true);

        service.remove(TEACHER_ID, exceptionId);

        verify(exceptions).deleteByIdAndTeacher(exceptionId, TEACHER_ID);
    }

    @Test
    void anAccountThatOwnsNoTeacherProfileCannotAddAnException() {
        UUID somebodyElse = UUID.randomUUID();
        when(teacherProfile.byUserId(somebodyElse)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(somebodyElse,
                new NewAvailabilityOverride(DATE, null, null, "BLOCK")))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(exceptions, never()).save(any());
    }

    @Test
    void anExtraWithoutHoursIsRejectedBeforeItIsSaved() {
        assertThatThrownBy(() -> service.add(TEACHER_ID,
                new NewAvailabilityOverride(DATE, null, null, "EXTRA")))
                .isInstanceOf(InvalidAvailabilityException.class);

        verify(exceptions, never()).save(any());
    }

    @Test
    void anUnknownTypeIsRejectedBeforeItIsSaved() {
        assertThatThrownBy(() -> service.add(TEACHER_ID, new NewAvailabilityOverride(DATE,
                LocalTime.of(10, 0), LocalTime.of(12, 0), "HOLIDAY")))
                .isInstanceOf(InvalidAvailabilityException.class);

        verify(exceptions, never()).save(any());
    }
}

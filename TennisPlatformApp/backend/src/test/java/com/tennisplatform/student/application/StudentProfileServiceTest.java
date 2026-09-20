package com.tennisplatform.student.application;

import com.tennisplatform.student.application.port.in.StudentProfileUpdate;
import com.tennisplatform.student.application.port.in.StudentProfileView;
import com.tennisplatform.student.application.service.StudentProfileService;
import com.tennisplatform.student.application.port.out.StudentProfileRepository;
import com.tennisplatform.student.domain.InvalidStudentProfileException;
import com.tennisplatform.student.domain.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID STUDENT_ID = UUID.randomUUID();

    private StudentProfileRepository profiles;
    private StudentProfileService service;

    @BeforeEach
    void setUp() {
        profiles = mock(StudentProfileRepository.class);
        service = new StudentProfileService(profiles, Clock.fixed(NOW, ZoneOffset.UTC));
        when(profiles.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    /**
     * The profile is born here, on the first save, and not at registration: the sign-up flow
     * asks for the personal data after the email has been verified
     * (16-fase6-analisis-perfiles.md).
     */
    @Test
    void theFirstUpdateCreatesTheProfile() {
        when(profiles.findByUserId(STUDENT_ID)).thenReturn(Optional.empty());

        StudentProfileView view = service.update(STUDENT_ID,
                new StudentProfileUpdate("Ana Ruiz", "600123123", null, null));

        assertThat(view.userId()).isEqualTo(STUDENT_ID);
        assertThat(view.fullName()).isEqualTo("Ana Ruiz");
        assertThat(view.phone()).isEqualTo("600123123");
    }

    /** The column is NOT NULL, so the first save cannot be a partial one. */
    @Test
    void theFirstUpdateHasToCarryTheName() {
        when(profiles.findByUserId(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(STUDENT_ID,
                new StudentProfileUpdate(null, "600123123", null, null)))
                .isInstanceOf(InvalidStudentProfileException.class)
                .hasMessageContaining("full name is required");
        verify(profiles, never()).save(any());
    }

    @Test
    void laterUpdatesMayBePartial() {
        when(profiles.findByUserId(STUDENT_ID)).thenReturn(Optional.of(
                StudentProfile.create(STUDENT_ID, "Ana Ruiz", "600123123", "12345678Z",
                        "Calle Mayor 1", NOW)));

        StudentProfileView view = service.update(STUDENT_ID,
                new StudentProfileUpdate(null, "600999888", null, null));

        assertThat(view.fullName()).isEqualTo("Ana Ruiz");
        assertThat(view.phone()).isEqualTo("600999888");
        assertThat(view.nationalId()).isEqualTo("12345678Z");
    }

    /**
     * A student who has not filled their data in yet is not an error: the empty answer is what
     * tells {@code /me} to report nulls so the frontend knows it has to ask.
     */
    @Test
    void readingAProfileThatDoesNotExistYetIsEmpty() {
        when(profiles.findByUserId(STUDENT_ID)).thenReturn(Optional.empty());

        assertThat(service.byUserId(STUDENT_ID)).isEmpty();
    }
}

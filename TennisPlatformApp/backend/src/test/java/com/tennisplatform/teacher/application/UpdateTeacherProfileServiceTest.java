package com.tennisplatform.teacher.application;

import com.tennisplatform.teacher.application.port.in.TeacherProfileUpdate;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import com.tennisplatform.teacher.application.port.out.TeacherProfileRepository;
import com.tennisplatform.teacher.application.service.UpdateTeacherProfileService;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.teacher.domain.TeacherProfile;
import com.tennisplatform.teacher.domain.TeacherProfileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateTeacherProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID TEACHER_ID = UUID.randomUUID();

    private TeacherProfileRepository profiles;
    private UpdateTeacherProfileService service;
    private TeacherProfile profile;

    @BeforeEach
    void setUp() {
        profiles = mock(TeacherProfileRepository.class);
        service = new UpdateTeacherProfileService(profiles);
        profile = TeacherProfile.create(TEACHER_ID, "Ana Serrano", null, "Europe/Madrid", NOW);

        when(profiles.findByUserId(TEACHER_ID)).thenReturn(Optional.of(profile));
        when(profiles.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void theTeacherCanChangeTheirOwnProfile() {
        TeacherProfileView updated = service.update(TEACHER_ID,
                new TeacherProfileUpdate("Ana S.", "600123123", "America/Bogota"));

        assertThat(updated.displayName()).isEqualTo("Ana S.");
        assertThat(updated.phone()).isEqualTo("600123123");
        assertThat(updated.timezone()).isEqualTo("America/Bogota");
    }

    /**
     * Criterion of Fase 6: holding the TEACHER role is not enough. The caller must own the
     * profile, which is what a stolen or hand-crafted token with the right role would not.
     */
    @Test
    void someoneElseCannotChangeTheTeacherProfile() {
        UUID somebodyElse = UUID.randomUUID();
        when(profiles.findByUserId(somebodyElse)).thenReturn(Optional.empty());
        when(profiles.findTheTeacher()).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.update(somebodyElse,
                new TeacherProfileUpdate("Impostor", null, null)))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(profiles, never()).save(any());
    }

    /** Told apart from the case above on purpose: 404 is a deployment problem, 403 is an attempt. */
    @Test
    void reportsThatNoTeacherExistsWhenTheBootstrapHasNotRun() {
        UUID anyone = UUID.randomUUID();
        when(profiles.findByUserId(anyone)).thenReturn(Optional.empty());
        when(profiles.findTheTeacher()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(anyone, new TeacherProfileUpdate("X", null, null)))
                .isInstanceOf(TeacherProfileNotFoundException.class);
    }
}

package com.tennisplatform.lesson.application;

import com.tennisplatform.availability.application.port.in.QueryAvailability;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.in.NewLesson;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.application.service.ScheduleLessonService;
import com.tennisplatform.lesson.domain.InvalidLessonException;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonOutsideAvailabilityException;
import com.tennisplatform.lesson.domain.LessonOverlapException;
import com.tennisplatform.lesson.domain.TeacherRoleRequiredException;
import com.tennisplatform.platform.application.port.in.GetMaxGroupCapacity;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
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

class ScheduleLessonServiceTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final Instant STARTS = Instant.parse("2026-06-01T09:00:00Z");
    private static final Instant ENDS = Instant.parse("2026-06-01T10:00:00Z");

    private LessonRepository lessons;
    private QueryAvailability availability;
    private ScheduleLessonService service;

    @BeforeEach
    void setUp() {
        lessons = mock(LessonRepository.class);
        availability = mock(QueryAvailability.class);
        GetTeacherProfile teacherProfile = mock(GetTeacherProfile.class);
        GetMaxGroupCapacity groupCapacity = mock(GetMaxGroupCapacity.class);

        when(teacherProfile.byUserId(TEACHER_ID)).thenReturn(Optional.of(
                new TeacherProfileView(TEACHER_ID, "The teacher", null, "Europe/Madrid")));
        when(groupCapacity.maxGroupCapacity()).thenReturn(8);
        when(lessons.existsOverlapping(any(), any(), any())).thenReturn(false);
        when(lessons.save(any())).thenAnswer(call -> call.getArgument(0));

        service = new ScheduleLessonService(lessons, teacherProfile, availability, groupCapacity,
                Clock.fixed(STARTS.minusSeconds(86_400), ZoneOffset.UTC));
    }

    @Test
    void schedulesALessonInsideTheConfiguredHours() {
        when(availability.covers(TEACHER_ID, STARTS, ENDS)).thenReturn(true);

        LessonView lesson = service.schedule(TEACHER_ID, request(1, false));

        assertThat(lesson.status()).isEqualTo("OPEN");
        assertThat(lesson.createdOutsideAvailability()).isFalse();
        verify(lessons).save(any(Lesson.class));
    }

    @Test
    void refusesALessonOutsideTheConfiguredHoursWhenItWasNotAskedForOnPurpose() {
        when(availability.covers(TEACHER_ID, STARTS, ENDS)).thenReturn(false);

        assertThatThrownBy(() -> service.schedule(TEACHER_ID, request(1, false)))
                .isInstanceOf(LessonOutsideAvailabilityException.class);

        verify(lessons, never()).save(any());
    }

    @Test
    void schedulesOutsideTheConfiguredHoursWhenAskedForOnPurpose() {
        when(availability.covers(TEACHER_ID, STARTS, ENDS)).thenReturn(false);

        LessonView lesson = service.schedule(TEACHER_ID, request(1, true));

        assertThat(lesson.createdOutsideAvailability()).isTrue();
    }

    /**
     * The flag records what happened, not what was asked for: a lesson that falls inside the
     * hours is not "outside" them just because the caller was prepared for it to be.
     */
    @Test
    void doesNotMarkALessonAsOutsideTheHoursWhenItIsInsideThem() {
        when(availability.covers(TEACHER_ID, STARTS, ENDS)).thenReturn(true);

        assertThat(service.schedule(TEACHER_ID, request(1, true)).createdOutsideAvailability())
                .isFalse();
    }

    @Test
    void refusesALessonThatOverlapsAnotherOne() {
        when(availability.covers(TEACHER_ID, STARTS, ENDS)).thenReturn(true);
        when(lessons.existsOverlapping(TEACHER_ID, STARTS, ENDS)).thenReturn(true);

        assertThatThrownBy(() -> service.schedule(TEACHER_ID, request(1, false)))
                .isInstanceOf(LessonOverlapException.class);

        verify(lessons, never()).save(any());
    }

    @Test
    void refusesACallerWhoIsNotTheTeacher() {
        assertThatThrownBy(() -> service.schedule(UUID.randomUUID(), request(1, false)))
                .isInstanceOf(TeacherRoleRequiredException.class);

        verify(availability, never()).covers(any(), any(), any());
    }

    @Test
    void refusesAGroupLargerThanTheConfiguredCap() {
        when(availability.covers(TEACHER_ID, STARTS, ENDS)).thenReturn(true);

        assertThatThrownBy(() -> service.schedule(TEACHER_ID,
                new NewLesson("GROUP", STARTS, ENDS, 9, null, false)))
                .isInstanceOf(InvalidLessonException.class);
    }

    @Test
    void refusesAnUnknownLessonType() {
        assertThatThrownBy(() -> service.schedule(TEACHER_ID,
                new NewLesson("PRIVATE", STARTS, ENDS, 1, null, false)))
                .isInstanceOf(InvalidLessonException.class);
    }

    private static NewLesson request(int capacity, boolean overrideAvailability) {
        return new NewLesson("INDIVIDUAL", STARTS, ENDS, capacity, null, overrideAvailability);
    }
}

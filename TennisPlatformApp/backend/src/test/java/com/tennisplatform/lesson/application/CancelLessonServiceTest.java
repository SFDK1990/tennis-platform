package com.tennisplatform.lesson.application;

import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.application.service.CancelLessonService;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonNotFoundException;
import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.lesson.domain.TeacherRoleRequiredException;
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

class CancelLessonServiceTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID LESSON_ID = UUID.randomUUID();
    private static final Instant STARTS = Instant.parse("2026-06-01T09:00:00Z");
    private static final Instant ENDS = Instant.parse("2026-06-01T10:00:00Z");

    /** Ten hours before the lesson: inside the 24-hour window, which the teacher is not bound by. */
    private static final Instant NOW = STARTS.minusSeconds(10 * 3600);

    private LessonRepository lessons;
    private CancelLessonService service;

    @BeforeEach
    void setUp() {
        lessons = mock(LessonRepository.class);
        GetTeacherProfile teacherProfile = mock(GetTeacherProfile.class);

        when(teacherProfile.byUserId(TEACHER_ID)).thenReturn(Optional.of(
                new TeacherProfileView(TEACHER_ID, "The teacher", null, "Europe/Madrid")));
        when(lessons.save(any())).thenAnswer(call -> call.getArgument(0));

        service = new CancelLessonService(lessons, teacherProfile, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * The teacher is not bound by the 24-hour window over their own lesson: one who falls ill
     * the night before has to be able to cancel. What the window decides is only whether this
     * counts as short notice, which Fase 9 will use to work out who needs telling.
     */
    @Test
    void cancelsAtShortNoticeAndSaysSo() {
        when(lessons.findById(LESSON_ID)).thenReturn(Optional.of(lessonOf(TEACHER_ID)));

        LessonView cancelled = service.cancel(TEACHER_ID, LESSON_ID);

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.cancelledAt()).isEqualTo(NOW);
        assertThat(cancelled.cancelledAtShortNotice()).isTrue();
    }

    @Test
    void failsWhenThereIsNoSuchLesson() {
        when(lessons.findById(LESSON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(TEACHER_ID, LESSON_ID))
                .isInstanceOf(LessonNotFoundException.class);
    }

    /**
     * Somebody else's lesson answers "not found" rather than "forbidden". With a single teacher
     * the case is unreachable; the day it is not, telling a caller that an id exists but is not
     * theirs is telling them something they had no way to know.
     */
    @Test
    void treatsAnotherTeachersLessonAsNotFound() {
        when(lessons.findById(LESSON_ID)).thenReturn(Optional.of(lessonOf(UUID.randomUUID())));

        assertThatThrownBy(() -> service.cancel(TEACHER_ID, LESSON_ID))
                .isInstanceOf(LessonNotFoundException.class);

        verify(lessons, never()).save(any());
    }

    @Test
    void refusesACallerWhoIsNotTheTeacher() {
        assertThatThrownBy(() -> service.cancel(UUID.randomUUID(), LESSON_ID))
                .isInstanceOf(TeacherRoleRequiredException.class);

        verify(lessons, never()).findById(any());
    }

    private static Lesson lessonOf(UUID teacherUserId) {
        return Lesson.rehydrate(LESSON_ID, teacherUserId, LessonType.INDIVIDUAL, STARTS, ENDS, 1,
                null, false, null);
    }
}

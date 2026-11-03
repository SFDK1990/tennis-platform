package com.tennisplatform.student.application;

import com.tennisplatform.student.application.port.in.ObjectiveAchieved;
import com.tennisplatform.student.application.port.out.AchievementRepository;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.application.service.StudentProgressService;
import com.tennisplatform.student.domain.AchievedOnInFutureException;
import com.tennisplatform.student.domain.ManagedStatus;
import com.tennisplatform.student.domain.ManagedStudent;
import com.tennisplatform.student.domain.ObjectiveAchievement;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.LevelView;
import com.tennisplatform.teacher.application.port.in.ObjectiveView;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The day of a mark is the teacher's, not the server's (44-fase29-analisis-progreso.md). */
class StudentProgressServiceTest {

    /** 23:30 in UTC on 2 November is already 00:30 on the 3rd in Madrid. */
    private static final Instant NOW = Instant.parse("2026-11-02T23:30:00Z");
    private static final UUID TEACHER = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID LEVEL = UUID.randomUUID();
    private static final UUID OBJECTIVE = UUID.randomUUID();

    private AchievementRepository achievements;
    private ApplicationEventPublisher events;
    private StudentProgressService service;

    @BeforeEach
    void setUp() {
        ManagedStudentRepository relationships = mock(ManagedStudentRepository.class);
        achievements = mock(AchievementRepository.class);
        QueryLevels levels = mock(QueryLevels.class);
        GetTeacherProfile teachers = mock(GetTeacherProfile.class);
        events = mock(ApplicationEventPublisher.class);
        ObjectiveView objective = new ObjectiveView(OBJECTIVE, LEVEL, "Saque", 1, false);

        when(relationships.findByPair(TEACHER, STUDENT)).thenReturn(Optional.of(ManagedStudent.rehydrate(
                UUID.randomUUID(), TEACHER, STUDENT, ManagedStatus.MANAGED, NOW, null, LEVEL, null, null)));
        when(levels.liveObjective(OBJECTIVE, TEACHER)).thenReturn(Optional.of(objective));
        when(levels.levelsOf(TEACHER)).thenReturn(List.of(new LevelView(LEVEL, "Iniciación", 1, List.of(objective))));
        when(levels.allObjectivesOf(TEACHER)).thenReturn(List.of(objective));
        when(teachers.byUserId(TEACHER)).thenReturn(Optional.of(
                new TeacherProfileView(TEACHER, "Marcos", null, "Europe/Madrid")));
        when(achievements.find(STUDENT, OBJECTIVE)).thenReturn(Optional.empty());

        service = new StudentProgressService(relationships, achievements, levels, teachers, events,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void withoutADayTheMarkIsTodayInTheTeachersZone() {
        service.mark(TEACHER, STUDENT, OBJECTIVE, null);

        verify(achievements).save(new ObjectiveAchievement(STUDENT, OBJECTIVE, LocalDate.parse("2026-11-03")));
        verify(events).publishEvent(new ObjectiveAchieved(TEACHER, STUDENT, OBJECTIVE));
    }

    @Test
    void theTeachersTodayIsNotInTheFutureButTheirTomorrowIs() {
        service.mark(TEACHER, STUDENT, OBJECTIVE, LocalDate.parse("2026-11-03"));

        assertThatThrownBy(() -> service.mark(TEACHER, STUDENT, OBJECTIVE, LocalDate.parse("2026-11-04")))
                .isInstanceOf(AchievedOnInFutureException.class);
    }

    @Test
    void markingAnAchievedObjectiveAgainTellsNobody() {
        when(achievements.find(STUDENT, OBJECTIVE)).thenReturn(Optional.of(
                new ObjectiveAchievement(STUDENT, OBJECTIVE, LocalDate.parse("2026-10-01"))));

        service.mark(TEACHER, STUDENT, OBJECTIVE, LocalDate.parse("2026-10-02"));

        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void aTeacherWithoutAnyStudentsHasNobodyWithTheLevelCompleted() {
        assertThat(service.withLevelCompleted(TEACHER, List.of())).isEmpty();
    }
}

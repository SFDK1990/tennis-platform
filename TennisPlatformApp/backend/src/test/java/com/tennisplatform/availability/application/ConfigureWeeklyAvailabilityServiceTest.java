package com.tennisplatform.availability.application;

import com.tennisplatform.availability.application.port.in.WeeklyRuleCommand;
import com.tennisplatform.availability.application.port.out.AvailabilityRuleRepository;
import com.tennisplatform.availability.application.service.ConfigureWeeklyAvailabilityService;
import com.tennisplatform.availability.domain.InvalidAvailabilityException;
import com.tennisplatform.availability.domain.OverlappingAvailabilityRulesException;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigureWeeklyAvailabilityServiceTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();

    private AvailabilityRuleRepository rules;
    private GetTeacherProfile teacherProfile;
    private ConfigureWeeklyAvailabilityService service;

    @BeforeEach
    void setUp() {
        rules = mock(AvailabilityRuleRepository.class);
        teacherProfile = mock(GetTeacherProfile.class);
        when(teacherProfile.byUserId(TEACHER_ID)).thenReturn(Optional.of(
                new TeacherProfileView(TEACHER_ID, "The teacher", null, "Europe/Madrid")));
        when(rules.replaceAllForTeacher(any(), anyList())).thenReturn(List.of());

        service = new ConfigureWeeklyAvailabilityService(rules, teacherProfile);
    }

    /**
     * Criterion 4. The point is the second assertion: a rejected set must leave the previous
     * configuration untouched, which only holds if the whole set is validated before anything
     * is written.
     */
    @Test
    void overlappingRulesAreRejectedAndNothingIsWritten() {
        assertThatThrownBy(() -> service.replace(TEACHER_ID, List.of(
                rule("MONDAY", 9, 13), rule("MONDAY", 12, 15))))
                .isInstanceOf(OverlappingAvailabilityRulesException.class);

        verify(rules, never()).replaceAllForTeacher(any(), anyList());
    }

    /** The clash is found wherever it sits in the list, not only between the first two. */
    @Test
    void anOverlapBetweenTheFirstAndTheLastRuleIsStillFound() {
        assertThatThrownBy(() -> service.replace(TEACHER_ID, List.of(
                rule("MONDAY", 9, 13), rule("TUESDAY", 9, 13), rule("WEDNESDAY", 9, 13),
                rule("MONDAY", 10, 11))))
                .isInstanceOf(OverlappingAvailabilityRulesException.class);

        verify(rules, never()).replaceAllForTeacher(any(), anyList());
    }

    /** Criterion 5. */
    @Test
    void adjacentRulesAreAccepted() {
        assertThatCode(() -> service.replace(TEACHER_ID, List.of(
                rule("MONDAY", 9, 11), rule("MONDAY", 11, 13))))
                .doesNotThrowAnyException();

        verify(rules).replaceAllForTeacher(any(), anyList());
    }

    /** An empty set is how a teacher clears their week, not an error. */
    @Test
    void anEmptySetIsAccepted() {
        assertThatCode(() -> service.replace(TEACHER_ID, List.of())).doesNotThrowAnyException();

        verify(rules).replaceAllForTeacher(any(), anyList());
    }

    /**
     * Ownership, not only the role the controller checked: an account that is not the teacher
     * owns no schedule, so there is nothing for it to replace.
     */
    @Test
    void anAccountThatOwnsNoTeacherProfileCannotConfigureAnything() {
        UUID somebodyElse = UUID.randomUUID();
        when(teacherProfile.byUserId(somebodyElse)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replace(somebodyElse, List.of(rule("MONDAY", 9, 13))))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(rules, never()).replaceAllForTeacher(any(), anyList());
    }

    @Test
    void anUnknownWeekdayIsRejectedBeforeAnythingIsWritten() {
        assertThatThrownBy(() -> service.replace(TEACHER_ID, List.of(
                new WeeklyRuleCommand("0", LocalTime.of(9, 0), LocalTime.of(13, 0), null, null))))
                .isInstanceOf(InvalidAvailabilityException.class);

        verify(rules, never()).replaceAllForTeacher(any(), anyList());
    }

    private static WeeklyRuleCommand rule(String day, int startHour, int endHour) {
        return new WeeklyRuleCommand(day, LocalTime.of(startHour, 0), LocalTime.of(endHour, 0),
                (LocalDate) null, (LocalDate) null);
    }
}

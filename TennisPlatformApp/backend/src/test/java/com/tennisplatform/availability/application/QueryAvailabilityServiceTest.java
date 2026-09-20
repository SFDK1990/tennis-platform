package com.tennisplatform.availability.application;

import com.tennisplatform.availability.application.port.out.AvailabilityOverrideRepository;
import com.tennisplatform.availability.application.port.out.AvailabilityRuleRepository;
import com.tennisplatform.availability.application.service.QueryAvailabilityService;
import com.tennisplatform.availability.domain.WeeklyAvailabilityRule;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryAvailabilityServiceTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();

    private AvailabilityRuleRepository rules;
    private AvailabilityOverrideRepository overrides;
    private GetTeacherProfile teacherProfile;
    private QueryAvailabilityService service;

    @BeforeEach
    void setUp() {
        rules = mock(AvailabilityRuleRepository.class);
        overrides = mock(AvailabilityOverrideRepository.class);
        teacherProfile = mock(GetTeacherProfile.class);

        when(teacherProfile.byUserId(TEACHER_ID)).thenReturn(Optional.of(
                new TeacherProfileView(TEACHER_ID, "The teacher", null, "Europe/Madrid")));
        when(rules.findByTeacher(TEACHER_ID)).thenReturn(List.of(
                WeeklyAvailabilityRule.create(TEACHER_ID, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(13, 0), null, null)));
        when(overrides.findByTeacherBetween(any(), any(), any())).thenReturn(List.of());

        service = new QueryAvailabilityService(rules, overrides, teacherProfile);
    }

    @Test
    void resolvesTheScheduleOfATeacher() {
        assertThat(service.covers(TEACHER_ID, Instant.parse("2026-01-05T09:00:00Z"),
                Instant.parse("2026-01-05T10:00:00Z"))).isTrue();
        assertThat(service.intervals(TEACHER_ID, Instant.parse("2026-01-05T00:00:00Z"),
                Instant.parse("2026-01-05T23:00:00Z"))).hasSize(1);
    }

    /**
     * An id that belongs to no teacher is available for nothing.
     *
     * <p>This used to fall back to the single teacher's time zone. With one teacher the fallback
     * was unreachable, so nothing would have caught it; with two it would have resolved one
     * teacher's rules against another's clock - a wrong answer rather than a failure. The
     * repositories are never even asked, which is what makes the answer cheap as well as right.
     */
    @Test
    void anIdThatBelongsToNoTeacherIsAvailableForNothing() {
        UUID somebodyElse = UUID.randomUUID();
        when(teacherProfile.byUserId(somebodyElse)).thenReturn(Optional.empty());

        assertThat(service.covers(somebodyElse, Instant.parse("2026-01-05T09:00:00Z"),
                Instant.parse("2026-01-05T10:00:00Z"))).isFalse();
        assertThat(service.intervals(somebodyElse, Instant.parse("2026-01-05T00:00:00Z"),
                Instant.parse("2026-01-05T23:00:00Z"))).isEmpty();

        verify(rules, never()).findByTeacher(somebodyElse);
        verify(teacherProfile, never()).get();
    }
}

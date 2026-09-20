package com.tennisplatform.availability.application.service;

import com.tennisplatform.availability.application.port.in.AvailabilityIntervalView;
import com.tennisplatform.availability.application.port.in.QueryAvailability;
import com.tennisplatform.availability.application.port.out.AvailabilityOverrideRepository;
import com.tennisplatform.availability.application.port.out.AvailabilityRuleRepository;
import com.tennisplatform.availability.domain.AvailabilitySchedule;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * The resolved side of the module: what {@code lesson} and {@code calendar} will ask.
 *
 * <p>Both answers come out of the same {@link AvailabilitySchedule}, so "is this slot free" and
 * "what is free this week" can never disagree with each other.
 */
public class QueryAvailabilityService implements QueryAvailability {

    private final AvailabilityRuleRepository rules;
    private final AvailabilityOverrideRepository exceptions;
    private final GetTeacherProfile teacherProfile;

    public QueryAvailabilityService(AvailabilityRuleRepository rules,
                                    AvailabilityOverrideRepository exceptions,
                                    GetTeacherProfile teacherProfile) {
        this.rules = rules;
        this.exceptions = exceptions;
        this.teacherProfile = teacherProfile;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean covers(UUID teacherUserId, Instant from, Instant to) {
        return scheduleAround(teacherUserId, from, to).covers(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityIntervalView> intervals(UUID teacherUserId, Instant from, Instant to) {
        ZoneId zone = zoneOf(teacherUserId);
        return scheduleAround(teacherUserId, from, to)
                .resolve(from.atZone(zone).toLocalDate(), to.atZone(zone).toLocalDate()).stream()
                .map(AvailabilityIntervalView::from)
                .toList();
    }

    /**
     * Loads the exceptions of the days the range touches, with a day of slack at each end: which
     * local date an instant falls on depends on the offset in force, and the interval that
     * covers an early morning can be the one stored under the previous date.
     */
    private AvailabilitySchedule scheduleAround(UUID teacherUserId, Instant from, Instant to) {
        ZoneId zone = zoneOf(teacherUserId);
        LocalDate firstDay = from.atZone(zone).toLocalDate().minusDays(1);
        LocalDate lastDay = to.atZone(zone).toLocalDate().plusDays(1);

        return AvailabilitySchedule.of(rules.findByTeacher(teacherUserId),
                exceptions.findByTeacherBetween(teacherUserId, firstDay, lastDay), zone);
    }

    private ZoneId zoneOf(UUID teacherUserId) {
        TeacherProfileView profile = teacherProfile.byUserId(teacherUserId)
                .orElseGet(teacherProfile::get);
        return ZoneId.of(profile.timezone());
    }
}

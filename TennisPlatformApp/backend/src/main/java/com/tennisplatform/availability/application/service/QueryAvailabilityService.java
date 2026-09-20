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
import java.util.Optional;
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
        return zoneOf(teacherUserId)
                .map(zone -> scheduleAround(teacherUserId, zone, from, to).covers(from, to))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityIntervalView> intervals(UUID teacherUserId, Instant from, Instant to) {
        return zoneOf(teacherUserId)
                .map(zone -> scheduleAround(teacherUserId, zone, from, to)
                        .resolve(from.atZone(zone).toLocalDate(), to.atZone(zone).toLocalDate())
                        .stream()
                        .map(AvailabilityIntervalView::from)
                        .toList())
                .orElseGet(List::of);
    }

    /**
     * Loads the exceptions of the days the range touches, over the same padded window the
     * schedule resolves against - the padding rule lives in {@link AvailabilitySchedule} so that
     * what is loaded and what is resolved cannot drift apart.
     */
    private AvailabilitySchedule scheduleAround(UUID teacherUserId, ZoneId zone,
                                                Instant from, Instant to) {
        LocalDate firstDay = AvailabilitySchedule.firstDayAround(from, zone);
        LocalDate lastDay = AvailabilitySchedule.lastDayAround(to, zone);

        return AvailabilitySchedule.of(rules.findByTeacher(teacherUserId),
                exceptions.findByTeacherBetween(teacherUserId, firstDay, lastDay), zone);
    }

    /**
     * Empty when the id belongs to no teacher, which is the honest answer: an account with no
     * schedule is available for nothing.
     *
     * <p>This used to fall back to the single teacher's zone. With one teacher the fallback was
     * unreachable, and the day there are two it would have resolved one teacher's rules against
     * another's clock - a wrong answer rather than a failure, arriving long after the line that
     * caused it was written.
     */
    private Optional<ZoneId> zoneOf(UUID teacherUserId) {
        return teacherProfile.byUserId(teacherUserId)
                .map(TeacherProfileView::timezone)
                .map(ZoneId::of);
    }
}

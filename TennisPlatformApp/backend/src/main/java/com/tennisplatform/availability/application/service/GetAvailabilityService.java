package com.tennisplatform.availability.application.service;

import com.tennisplatform.availability.application.port.in.AvailabilityView;
import com.tennisplatform.availability.application.port.in.AvailabilityOverrideView;
import com.tennisplatform.availability.application.port.in.GetAvailability;
import com.tennisplatform.availability.application.port.in.WeeklyRuleView;
import com.tennisplatform.availability.application.port.out.AvailabilityOverrideRepository;
import com.tennisplatform.availability.application.port.out.AvailabilityRuleRepository;
import com.tennisplatform.availability.domain.AvailabilityDateRange;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

public class GetAvailabilityService implements GetAvailability {

    private final AvailabilityRuleRepository rules;
    private final AvailabilityOverrideRepository exceptions;
    private final GetTeacherProfile teacherProfile;

    public GetAvailabilityService(AvailabilityRuleRepository rules,
                                  AvailabilityOverrideRepository exceptions,
                                  GetTeacherProfile teacherProfile) {
        this.rules = rules;
        this.exceptions = exceptions;
        this.teacherProfile = teacherProfile;
    }

    /**
     * Readable by any authenticated caller, like the teacher's profile: a student has to know
     * when their teacher works. It exposes working hours, which is what the product is for, and
     * no personal data at all.
     */
    @Override
    @Transactional(readOnly = true)
    public AvailabilityView get(LocalDate from, LocalDate to) {
        AvailabilityDateRange range = new AvailabilityDateRange(from, to);
        UUID teacherUserId = teacherProfile.get().userId();

        return new AvailabilityView(
                rules.findByTeacher(teacherUserId).stream().map(WeeklyRuleView::from).toList(),
                exceptions.findByTeacherBetween(teacherUserId, range.from(), range.to()).stream()
                        .map(AvailabilityOverrideView::from).toList());
    }
}

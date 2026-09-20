package com.tennisplatform.availability.application.service;

import com.tennisplatform.availability.application.port.in.AvailabilityOverrideView;
import com.tennisplatform.availability.application.port.in.ManageAvailabilityExceptions;
import com.tennisplatform.availability.application.port.in.NewAvailabilityOverride;
import com.tennisplatform.availability.application.port.out.AvailabilityOverrideRepository;
import com.tennisplatform.availability.domain.AvailabilityOverrideNotFoundException;
import com.tennisplatform.availability.domain.AvailabilityOverrideType;
import com.tennisplatform.availability.domain.AvailabilityOverride;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class ManageAvailabilityExceptionsService implements ManageAvailabilityExceptions {

    private final AvailabilityOverrideRepository exceptions;
    private final TeacherSchedules schedules;

    public ManageAvailabilityExceptionsService(AvailabilityOverrideRepository exceptions,
                                               GetTeacherProfile teacherProfile) {
        this.exceptions = exceptions;
        this.schedules = new TeacherSchedules(teacherProfile);
    }

    @Override
    @Transactional
    public AvailabilityOverrideView add(UUID teacherUserId, NewAvailabilityOverride command) {
        schedules.requireTheTeacher(teacherUserId);

        return AvailabilityOverrideView.from(exceptions.save(AvailabilityOverride.create(
                teacherUserId, command.date(), command.startTime(), command.endTime(),
                AvailabilityOverrideType.parse(command.type()))));
    }

    /**
     * Deletes by id <em>and</em> teacher, so an id belonging to somebody else is simply not
     * found. Loading it first to compare owners would answer 403 and thereby confirm the id
     * exists, which is more than the caller has earned.
     */
    @Override
    @Transactional
    public void remove(UUID teacherUserId, UUID exceptionId) {
        schedules.requireTheTeacher(teacherUserId);

        if (!exceptions.deleteByIdAndTeacher(exceptionId, teacherUserId)) {
            throw new AvailabilityOverrideNotFoundException("No availability exception with that id");
        }
    }
}

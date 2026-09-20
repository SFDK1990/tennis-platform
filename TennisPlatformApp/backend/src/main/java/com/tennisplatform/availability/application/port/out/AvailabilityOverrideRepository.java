package com.tennisplatform.availability.application.port.out;

import com.tennisplatform.availability.domain.AvailabilityOverride;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvailabilityOverrideRepository {

    /** Both dates included. */
    List<AvailabilityOverride> findByTeacherBetween(UUID teacherUserId, LocalDate from, LocalDate to);

    AvailabilityOverride save(AvailabilityOverride exception);

    /** False when nothing was removed, which covers both "no such id" and "not yours". */
    boolean deleteByIdAndTeacher(UUID id, UUID teacherUserId);
}

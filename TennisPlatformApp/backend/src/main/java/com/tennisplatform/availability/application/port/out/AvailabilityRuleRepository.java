package com.tennisplatform.availability.application.port.out;

import com.tennisplatform.availability.domain.WeeklyAvailabilityRule;

import java.util.List;
import java.util.UUID;

public interface AvailabilityRuleRepository {

    List<WeeklyAvailabilityRule> findByTeacher(UUID teacherUserId);

    /**
     * Swaps the teacher's whole set in one go. Expressed as a single operation rather than a
     * delete followed by a save so the adapter, not each caller, is responsible for the two
     * halves happening inside one transaction.
     */
    List<WeeklyAvailabilityRule> replaceAllForTeacher(UUID teacherUserId,
                                                      List<WeeklyAvailabilityRule> rules);
}

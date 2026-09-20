package com.tennisplatform.availability.adapters.out.persistence;

import com.tennisplatform.availability.application.port.out.AvailabilityRuleRepository;
import com.tennisplatform.availability.domain.WeeklyAvailabilityRule;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class AvailabilityRuleRepositoryAdapter implements AvailabilityRuleRepository {

    private final WeeklyAvailabilityRuleJpaRepository jpa;

    AvailabilityRuleRepositoryAdapter(WeeklyAvailabilityRuleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<WeeklyAvailabilityRule> findByTeacher(UUID teacherUserId) {
        return jpa.findByTeacherUserIdOrderByDayOfWeekAscStartTimeAsc(teacherUserId).stream()
                .map(WeeklyAvailabilityRuleEntity::toDomain)
                .toList();
    }

    /**
     * Delete then insert, flushed in between.
     *
     * <p>Without the flush Hibernate is free to order the insert before the delete, and the old
     * rows would take the new ones with them. The caller's transaction is what makes the pair
     * atomic; this only makes the order inside it deterministic.
     */
    @Override
    public List<WeeklyAvailabilityRule> replaceAllForTeacher(UUID teacherUserId,
                                                             List<WeeklyAvailabilityRule> rules) {
        jpa.deleteByTeacherUserId(teacherUserId);
        jpa.flush();

        return jpa.saveAll(rules.stream().map(WeeklyAvailabilityRuleEntity::fromDomain).toList())
                .stream()
                .map(WeeklyAvailabilityRuleEntity::toDomain)
                .toList();
    }
}

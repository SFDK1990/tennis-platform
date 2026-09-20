package com.tennisplatform.availability.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface WeeklyAvailabilityRuleJpaRepository extends JpaRepository<WeeklyAvailabilityRuleEntity, UUID> {

    List<WeeklyAvailabilityRuleEntity> findByTeacherUserIdOrderByDayOfWeekAscStartTimeAsc(UUID teacherUserId);

    void deleteByTeacherUserId(UUID teacherUserId);
}

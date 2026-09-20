package com.tennisplatform.availability.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

interface AvailabilityOverrideJpaRepository
        extends JpaRepository<AvailabilityOverrideEntity, UUID> {

    List<AvailabilityOverrideEntity> findByTeacherUserIdAndDateBetweenOrderByDateAscStartTimeAsc(
            UUID teacherUserId, LocalDate from, LocalDate to);

    long deleteByIdAndTeacherUserId(UUID id, UUID teacherUserId);
}

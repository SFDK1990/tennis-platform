package com.tennisplatform.availability.adapters.out.persistence;

import com.tennisplatform.availability.application.port.out.AvailabilityOverrideRepository;
import com.tennisplatform.availability.domain.AvailabilityOverride;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
class AvailabilityOverrideRepositoryAdapter implements AvailabilityOverrideRepository {

    private final AvailabilityOverrideJpaRepository jpa;

    AvailabilityOverrideRepositoryAdapter(AvailabilityOverrideJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<AvailabilityOverride> findByTeacherBetween(UUID teacherUserId,
                                                                LocalDate from, LocalDate to) {
        return jpa.findByTeacherUserIdAndDateBetweenOrderByDateAscStartTimeAsc(teacherUserId, from, to)
                .stream()
                .map(AvailabilityOverrideEntity::toDomain)
                .toList();
    }

    @Override
    public AvailabilityOverride save(AvailabilityOverride exception) {
        return jpa.save(AvailabilityOverrideEntity.fromDomain(exception)).toDomain();
    }

    /** Deleting by both columns is what turns "not yours" into "not found" without a second query. */
    @Override
    public boolean deleteByIdAndTeacher(UUID id, UUID teacherUserId) {
        return jpa.deleteByIdAndTeacherUserId(id, teacherUserId) > 0;
    }
}

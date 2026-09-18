package com.tennisplatform.teacher.adapters.out.persistence;

import com.tennisplatform.teacher.application.port.out.TeacherProfileRepository;
import com.tennisplatform.teacher.domain.TeacherProfile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class TeacherProfileRepositoryAdapter implements TeacherProfileRepository {

    private final TeacherProfileJpaRepository jpa;

    TeacherProfileRepositoryAdapter(TeacherProfileJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * Ordering by creation date makes the result deterministic. There is only one row today -
     * the single teacher of the MVP - but an unordered "first" would quietly become arbitrary
     * the day there is more than one, and a flaky read is worse than an explicit rewrite.
     */
    @Override
    public Optional<TeacherProfile> findTheTeacher() {
        return jpa.findFirstByOrderByCreatedAtAsc().map(TeacherProfileEntity::toDomain);
    }

    @Override
    public Optional<TeacherProfile> findByUserId(UUID userId) {
        return jpa.findById(userId).map(TeacherProfileEntity::toDomain);
    }

    @Override
    public TeacherProfile save(TeacherProfile profile) {
        return jpa.save(TeacherProfileEntity.fromDomain(profile)).toDomain();
    }
}

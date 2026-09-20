package com.tennisplatform.student.adapters.out.persistence;

import com.tennisplatform.student.application.port.out.StudentProfileRepository;
import com.tennisplatform.student.domain.StudentProfile;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class StudentProfileRepositoryAdapter implements StudentProfileRepository {

    private final StudentProfileJpaRepository jpa;

    StudentProfileRepositoryAdapter(StudentProfileJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<StudentProfile> findByUserId(UUID userId) {
        return jpa.findById(userId).map(StudentProfileEntity::toDomain);
    }

    @Override
    public List<StudentProfile> findAllByUserIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return jpa.findAllById(userIds).stream().map(StudentProfileEntity::toDomain).toList();
    }

    @Override
    public StudentProfile save(StudentProfile profile) {
        return jpa.save(StudentProfileEntity.fromDomain(profile)).toDomain();
    }
}

package com.tennisplatform.student.adapters.out.persistence;

import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.domain.ManagedStatus;
import com.tennisplatform.student.domain.ManagedStudent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ManagedStudentRepositoryAdapter implements ManagedStudentRepository {

    private final ManagedStudentJpaRepository jpa;

    ManagedStudentRepositoryAdapter(ManagedStudentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<ManagedStudent> findByPair(UUID teacherUserId, UUID studentUserId) {
        return jpa.findByTeacherUserIdAndStudentUserId(teacherUserId, studentUserId)
                .map(ManagedStudentEntity::toDomain);
    }

    @Override
    public List<ManagedStudent> findAllByTeacher(UUID teacherUserId) {
        return jpa.findAllByTeacherUserIdOrderByManagedAtAsc(teacherUserId).stream()
                .map(ManagedStudentEntity::toDomain)
                .toList();
    }

    @Override
    public long countManagedBy(UUID teacherUserId) {
        return jpa.countByTeacherUserIdAndStatus(teacherUserId, ManagedStatus.MANAGED);
    }

    @Override
    public List<UUID> findManagingTeachers(UUID studentUserId) {
        return jpa.findAllByStudentUserIdAndStatus(studentUserId, ManagedStatus.MANAGED).stream()
                .map(entity -> entity.toDomain().teacherUserId())
                .toList();
    }

    @Override
    public ManagedStudent save(ManagedStudent relationship) {
        return jpa.save(ManagedStudentEntity.fromDomain(relationship)).toDomain();
    }
}

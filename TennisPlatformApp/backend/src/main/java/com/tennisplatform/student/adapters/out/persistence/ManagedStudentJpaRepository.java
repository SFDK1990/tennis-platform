package com.tennisplatform.student.adapters.out.persistence;

import com.tennisplatform.student.domain.ManagedStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ManagedStudentJpaRepository extends JpaRepository<ManagedStudentEntity, UUID> {

    Optional<ManagedStudentEntity> findByTeacherUserIdAndStudentUserId(UUID teacherUserId,
                                                                      UUID studentUserId);

    List<ManagedStudentEntity> findAllByTeacherUserIdOrderByManagedAtAsc(UUID teacherUserId);

    long countByTeacherUserIdAndStatus(UUID teacherUserId, ManagedStatus status);

    List<ManagedStudentEntity> findAllByStudentUserIdAndStatus(UUID studentUserId, ManagedStatus status);
}

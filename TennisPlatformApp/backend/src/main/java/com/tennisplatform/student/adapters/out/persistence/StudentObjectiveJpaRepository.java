package com.tennisplatform.student.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface StudentObjectiveJpaRepository extends JpaRepository<StudentObjectiveEntity, StudentObjectiveEntity.Key> {

    List<StudentObjectiveEntity> findAllByStudentUserId(UUID studentUserId);

    List<StudentObjectiveEntity> findAllByStudentUserIdIn(Collection<UUID> studentUserIds);

    @Modifying
    @Query("delete from StudentObjectiveEntity a where a.studentUserId = :studentUserId")
    void deleteAllOfStudent(UUID studentUserId);
}

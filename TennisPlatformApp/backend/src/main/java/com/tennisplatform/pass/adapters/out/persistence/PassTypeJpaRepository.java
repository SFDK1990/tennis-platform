package com.tennisplatform.pass.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PassTypeJpaRepository extends JpaRepository<PassTypeEntity, UUID> {

    List<PassTypeEntity> findAllByTeacherUserId(UUID teacherUserId);
}

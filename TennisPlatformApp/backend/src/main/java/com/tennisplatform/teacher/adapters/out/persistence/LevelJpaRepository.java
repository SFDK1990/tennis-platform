package com.tennisplatform.teacher.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface LevelJpaRepository extends JpaRepository<LevelEntity, UUID> {

    List<LevelEntity> findAllByTeacherUserIdOrderByPositionAsc(UUID teacherUserId);
}

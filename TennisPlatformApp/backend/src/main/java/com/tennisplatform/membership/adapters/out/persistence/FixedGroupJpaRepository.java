package com.tennisplatform.membership.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface FixedGroupJpaRepository extends JpaRepository<FixedGroupEntity, UUID> {

    List<FixedGroupEntity> findAllByTeacherUserIdOrderByNameAsc(UUID teacherUserId);
}

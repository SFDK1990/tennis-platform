package com.tennisplatform.teacher.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TeacherProfileJpaRepository extends JpaRepository<TeacherProfileEntity, UUID> {

    Optional<TeacherProfileEntity> findFirstByOrderByCreatedAtAsc();
}

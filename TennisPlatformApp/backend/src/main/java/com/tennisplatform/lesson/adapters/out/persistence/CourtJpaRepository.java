package com.tennisplatform.lesson.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CourtJpaRepository extends JpaRepository<CourtEntity, UUID> {

    List<CourtEntity> findAllByOrderByPositionAsc();

    Optional<CourtEntity> findFirstByOrderByPositionAsc();
}

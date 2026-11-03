package com.tennisplatform.teacher.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface LevelObjectiveJpaRepository extends JpaRepository<LevelObjectiveEntity, UUID> {

    List<LevelObjectiveEntity> findAllByLevelIdInOrderByLevelIdAscPositionAsc(Collection<UUID> levelIds);
}

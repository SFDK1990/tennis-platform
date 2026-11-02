package com.tennisplatform.teacher.adapters.out.persistence;

import com.tennisplatform.teacher.application.port.out.ObjectiveRepository;
import com.tennisplatform.teacher.domain.LevelObjective;
import com.tennisplatform.teacher.domain.ObjectiveAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ObjectiveRepositoryAdapter implements ObjectiveRepository {

    private static final String UNIQUE_VIOLATION = "23505";

    private final LevelObjectiveJpaRepository jpa;

    ObjectiveRepositoryAdapter(LevelObjectiveJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<LevelObjective> findByLevels(Collection<UUID> levelIds) {
        if (levelIds.isEmpty()) {
            return List.of();
        }
        return jpa.findAllByLevelIdInOrderByLevelIdAscPositionAsc(levelIds).stream()
                .map(LevelObjectiveEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<LevelObjective> findById(UUID id) {
        return jpa.findById(id).map(LevelObjectiveEntity::toDomain);
    }

    @Override
    public LevelObjective save(LevelObjective objective) {
        try {
            return jpa.saveAndFlush(LevelObjectiveEntity.fromDomain(objective)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause() instanceof SQLException cause && UNIQUE_VIOLATION.equals(cause.getSQLState())) {
                throw new ObjectiveAlreadyExistsException("The level already has an objective called "
                        + objective.title());
            }
            throw e;
        }
    }
}

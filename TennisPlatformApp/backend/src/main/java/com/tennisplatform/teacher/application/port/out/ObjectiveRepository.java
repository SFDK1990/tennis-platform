package com.tennisplatform.teacher.application.port.out;

import com.tennisplatform.teacher.domain.LevelObjective;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObjectiveRepository {

    /** Every objective of these levels, retired ones included, by level and position. */
    List<LevelObjective> findByLevels(Collection<UUID> levelIds);

    Optional<LevelObjective> findById(UUID id);

    /** Translates the unique live title into {@code ObjectiveAlreadyExistsException}. */
    LevelObjective save(LevelObjective objective);
}

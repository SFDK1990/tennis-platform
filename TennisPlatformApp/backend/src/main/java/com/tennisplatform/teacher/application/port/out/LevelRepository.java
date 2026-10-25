package com.tennisplatform.teacher.application.port.out;

import com.tennisplatform.teacher.domain.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LevelRepository {

    /** The teacher's levels in their order. */
    List<Level> findByTeacher(UUID teacherUserId);

    Optional<Level> findById(UUID id);

    /** Translates the unique name into {@code LevelAlreadyExistsException}. */
    Level save(Level level);

    /** Translates a foreign key that still points at the level into {@code LevelInUseException}. */
    void delete(UUID id);
}

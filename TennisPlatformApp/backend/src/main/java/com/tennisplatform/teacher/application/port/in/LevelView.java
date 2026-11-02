package com.tennisplatform.teacher.application.port.in;

import com.tennisplatform.teacher.domain.Level;

import java.util.List;
import java.util.UUID;

/** A level with its live objectives in their order (Fase 29); the retired ones are not in it. */
public record LevelView(UUID id, String name, int position, List<ObjectiveView> objectives) {

    public LevelView {
        objectives = List.copyOf(objectives);
    }

    public static LevelView from(Level level, List<ObjectiveView> objectives) {
        return new LevelView(level.id(), level.name(), level.position(), objectives);
    }
}

package com.tennisplatform.teacher.application.port.in;

import com.tennisplatform.teacher.domain.LevelObjective;

import java.util.UUID;

/** An objective of a level (Fase 29). {@code retired} is only ever true for another module's eyes. */
public record ObjectiveView(UUID id, UUID levelId, String title, int position, boolean retired) {

    public static ObjectiveView from(LevelObjective objective) {
        return new ObjectiveView(objective.id(), objective.levelId(), objective.title(), objective.position(),
                objective.isRetired());
    }
}

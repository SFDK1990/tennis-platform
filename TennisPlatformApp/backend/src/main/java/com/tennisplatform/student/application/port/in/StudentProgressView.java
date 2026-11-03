package com.tennisplatform.student.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A student's progress through the objectives of the levels (44-fase29-analisis-progreso.md), level by
 * level in the teacher's order.
 */
public record StudentProgressView(List<LevelProgress> levels) {

    public StudentProgressView {
        levels = List.copyOf(levels);
    }

    /**
     * @param current   whether it is the student's level now
     * @param completed whether the level has live objectives and the student has achieved them all
     */
    public record LevelProgress(UUID levelId, String levelName, boolean current, boolean completed,
                                List<ObjectiveProgress> objectives) {

        public LevelProgress {
            objectives = List.copyOf(objectives);
        }
    }

    /** {@code achievedOn} is null while it is pending. */
    public record ObjectiveProgress(UUID id, String title, LocalDate achievedOn) {
    }
}

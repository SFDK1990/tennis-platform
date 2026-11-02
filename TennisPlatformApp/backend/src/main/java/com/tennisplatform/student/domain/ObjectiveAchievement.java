package com.tennisplatform.student.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * An objective of a level that the teacher has marked as achieved for a student, and the day, in
 * the teacher's zone (44-fase29-analisis-progreso.md). Unmarking deletes it: there is no third state.
 */
public record ObjectiveAchievement(UUID studentUserId, UUID objectiveId, LocalDate achievedOn) {

    public ObjectiveAchievement {
        Objects.requireNonNull(studentUserId);
        Objects.requireNonNull(objectiveId);
        Objects.requireNonNull(achievedOn);
    }
}

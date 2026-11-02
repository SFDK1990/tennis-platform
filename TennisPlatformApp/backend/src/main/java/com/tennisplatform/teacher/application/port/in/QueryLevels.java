package com.tennisplatform.teacher.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What other modules ask about levels. */
public interface QueryLevels {

    /** Whether the level exists and is one of this teacher's: a lesson or a student cannot take another's. */
    boolean belongsTo(UUID levelId, UUID teacherUserId);

    /** The level's name, for an export; null if it does not exist. */
    String nameOf(UUID levelId);

    /** The teacher's levels in their order, each with its live objectives (Fase 29). */
    List<LevelView> levelsOf(UUID teacherUserId);

    /** Every objective of the teacher's levels, retired ones included: a student keeps what they achieved. */
    List<ObjectiveView> allObjectivesOf(UUID teacherUserId);

    /** The objective, if it is live and on one of this teacher's levels. */
    Optional<ObjectiveView> liveObjective(UUID objectiveId, UUID teacherUserId);

    /** Gives a new teacher the four levels every teacher starts with. Does nothing if they have some. */
    void seedDefaults(UUID teacherUserId);
}

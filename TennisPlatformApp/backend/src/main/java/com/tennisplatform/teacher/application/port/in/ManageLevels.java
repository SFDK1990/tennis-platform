package com.tennisplatform.teacher.application.port.in;

import java.util.List;
import java.util.UUID;

/** The teacher's levels (36-fase23-analisis-niveles-ficha.md, decision 1). */
public interface ManageLevels {

    /** The levels of the teacher, readable by anybody signed in: a student sees which lesson is for whom. */
    List<LevelView> ofTheTeacher();

    LevelView create(UUID teacherUserId, String name);

    /** {@code name} or {@code position} may be null, meaning "leave as it is". The others move to make room. */
    LevelView update(UUID teacherUserId, UUID levelId, String name, Integer position);

    /** Refused with {@code LevelInUseException} while a student or a lesson has it. */
    void delete(UUID teacherUserId, UUID levelId);

    /** Adds an objective at the end of the level's list (44-fase29-analisis-progreso.md). */
    ObjectiveView addObjective(UUID teacherUserId, UUID levelId, String title);

    /** {@code title} or {@code position} may be null, meaning "leave as it is". The others move to make room. */
    ObjectiveView updateObjective(UUID teacherUserId, UUID objectiveId, String title, Integer position);

    /**
     * Takes the objective out of the level's list. It is retired rather than deleted, so a student who
     * achieved it keeps it; one nobody achieved is just as invisible afterwards.
     */
    void retireObjective(UUID teacherUserId, UUID objectiveId);
}

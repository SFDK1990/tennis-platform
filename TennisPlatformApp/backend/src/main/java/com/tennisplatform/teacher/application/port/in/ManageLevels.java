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
}

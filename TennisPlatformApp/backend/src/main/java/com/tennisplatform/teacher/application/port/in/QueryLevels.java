package com.tennisplatform.teacher.application.port.in;

import java.util.UUID;

/** What other modules ask about levels. */
public interface QueryLevels {

    /** Whether the level exists and is one of this teacher's: a lesson or a student cannot take another's. */
    boolean belongsTo(UUID levelId, UUID teacherUserId);

    /** The level's name, for an export; null if it does not exist. */
    String nameOf(UUID levelId);

    /** Gives a new teacher the four levels every teacher starts with. Does nothing if they have some. */
    void seedDefaults(UUID teacherUserId);
}

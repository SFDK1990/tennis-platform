package com.tennisplatform.teacher.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Something every student of a level works towards, which the teacher marks as achieved for each
 * of them (44-fase29-analisis-progreso.md). {@code position} orders the live objectives of the
 * level, lowest first. A retired objective is out of the level's list but stays for whoever
 * achieved it.
 */
public record LevelObjective(UUID id, UUID levelId, String title, int position, Instant retiredAt) {

    public static final int MAX_TITLE = 120;

    public LevelObjective {
        if (title == null || title.isBlank()) {
            throw new InvalidObjectiveException("An objective needs a title");
        }
        title = title.strip();
        if (title.length() > MAX_TITLE) {
            throw new InvalidObjectiveException("An objective's title has at most " + MAX_TITLE + " characters");
        }
    }

    public boolean isRetired() {
        return retiredAt != null;
    }

    public LevelObjective renamed(String newTitle) {
        return new LevelObjective(id, levelId, newTitle, position, retiredAt);
    }

    public LevelObjective at(int newPosition) {
        return new LevelObjective(id, levelId, title, newPosition, retiredAt);
    }

    public LevelObjective retired(Instant when) {
        return new LevelObjective(id, levelId, title, position, when);
    }
}

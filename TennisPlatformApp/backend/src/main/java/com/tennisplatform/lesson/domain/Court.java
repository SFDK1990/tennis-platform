package com.tennisplatform.lesson.domain;

import java.util.UUID;

/**
 * One of the school's tennis courts (40-fase25.6-analisis-pistas.md). There are two, and the
 * teacher only renames them: adding or removing courts comes with the club. {@code position} is
 * their order; the first one is where a lesson goes when nobody says otherwise.
 */
public record Court(UUID id, String name, int position) {

    public static final int MAX_NAME = 60;

    public Court {
        if (name == null || name.isBlank()) {
            throw new InvalidCourtException("A court needs a name");
        }
        name = name.trim();
        if (name.length() > MAX_NAME) {
            throw new InvalidCourtException("A court's name has at most " + MAX_NAME + " characters");
        }
    }

    public Court renamed(String newName) {
        return new Court(id, newName, position);
    }
}

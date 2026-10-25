package com.tennisplatform.teacher.domain;

import java.util.List;
import java.util.UUID;

/**
 * A level the teacher sorts students and lessons by (36-fase23-analisis-niveles-ficha.md). The
 * teacher names them; {@code position} is their order, lowest first.
 */
public record Level(UUID id, UUID teacherUserId, String name, int position) {

    public static final int MAX_NAME = 60;

    /** The four levels of 01-analisis-funcional.md section 18, which every teacher starts with. */
    public static final List<String> DEFAULTS = List.of("Iniciación", "Intermedio", "Avanzado", "Competición");

    public Level {
        if (name == null || name.isBlank()) {
            throw new InvalidLevelException("A level needs a name");
        }
        name = name.trim();
        if (name.length() > MAX_NAME) {
            throw new InvalidLevelException("A level's name has at most " + MAX_NAME + " characters");
        }
    }

    public Level renamed(String newName) {
        return new Level(id, teacherUserId, newName, position);
    }

    public Level at(int newPosition) {
        return new Level(id, teacherUserId, name, newPosition);
    }
}

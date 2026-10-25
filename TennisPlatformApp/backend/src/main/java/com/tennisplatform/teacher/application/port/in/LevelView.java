package com.tennisplatform.teacher.application.port.in;

import com.tennisplatform.teacher.domain.Level;

import java.util.UUID;

public record LevelView(UUID id, String name, int position) {

    public static LevelView from(Level level) {
        return new LevelView(level.id(), level.name(), level.position());
    }
}

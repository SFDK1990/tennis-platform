package com.tennisplatform.teacher.adapters.out.persistence;

import com.tennisplatform.teacher.domain.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "levels")
class LevelEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private int position;

    protected LevelEntity() {
    }

    static LevelEntity fromDomain(Level level) {
        LevelEntity entity = new LevelEntity();
        entity.id = level.id();
        entity.teacherUserId = level.teacherUserId();
        entity.name = level.name();
        entity.position = level.position();
        return entity;
    }

    Level toDomain() {
        return new Level(id, teacherUserId, name, position);
    }
}

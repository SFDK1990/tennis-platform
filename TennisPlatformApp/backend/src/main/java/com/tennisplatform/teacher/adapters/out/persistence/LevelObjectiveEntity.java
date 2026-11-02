package com.tennisplatform.teacher.adapters.out.persistence;

import com.tennisplatform.teacher.domain.LevelObjective;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "level_objectives")
class LevelObjectiveEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "level_id", nullable = false, updatable = false)
    private UUID levelId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false)
    private int position;

    @Column(name = "retired_at")
    private Instant retiredAt;

    protected LevelObjectiveEntity() {
    }

    static LevelObjectiveEntity fromDomain(LevelObjective objective) {
        LevelObjectiveEntity entity = new LevelObjectiveEntity();
        entity.id = objective.id();
        entity.levelId = objective.levelId();
        entity.title = objective.title();
        entity.position = objective.position();
        entity.retiredAt = objective.retiredAt();
        return entity;
    }

    LevelObjective toDomain() {
        return new LevelObjective(id, levelId, title, position, retiredAt);
    }
}

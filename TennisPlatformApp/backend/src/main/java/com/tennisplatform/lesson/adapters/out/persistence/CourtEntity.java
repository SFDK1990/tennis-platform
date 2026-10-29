package com.tennisplatform.lesson.adapters.out.persistence;

import com.tennisplatform.lesson.domain.Court;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "courts")
class CourtEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, updatable = false)
    private int position;

    protected CourtEntity() {
    }

    static CourtEntity fromDomain(Court court) {
        CourtEntity entity = new CourtEntity();
        entity.id = court.id();
        entity.name = court.name();
        entity.position = court.position();
        return entity;
    }

    Court toDomain() {
        return new Court(id, name, position);
    }
}

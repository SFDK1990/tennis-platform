package com.tennisplatform.pass.adapters.out.persistence;

import com.tennisplatform.pass.domain.PassLessonType;
import com.tennisplatform.pass.domain.PassType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "pass_types")
class PassTypeEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 20)
    private PassLessonType lessonType;

    @Column(nullable = false)
    private int lessons;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(name = "validity_months")
    private Integer validityMonths;

    @Column(nullable = false)
    private boolean retired;

    protected PassTypeEntity() {
    }

    static PassTypeEntity fromDomain(PassType type) {
        PassTypeEntity entity = new PassTypeEntity();
        entity.id = type.id();
        entity.teacherUserId = type.teacherUserId();
        entity.name = type.name();
        entity.lessonType = type.lessonType();
        entity.lessons = type.lessons();
        entity.priceCents = type.priceCents();
        entity.validityMonths = type.validityMonths();
        entity.retired = type.retired();
        return entity;
    }

    PassType toDomain() {
        return new PassType(id, teacherUserId, name, lessonType, lessons, priceCents, validityMonths, retired);
    }
}

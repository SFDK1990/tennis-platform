package com.tennisplatform.membership.adapters.out.persistence;

import com.tennisplatform.membership.domain.FixedGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fixed_groups")
class FixedGroupEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "monthly_fee_cents", nullable = false)
    private int monthlyFeeCents;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FixedGroupEntity() {
    }

    static FixedGroupEntity fromDomain(FixedGroup group) {
        FixedGroupEntity entity = new FixedGroupEntity();
        entity.id = group.id();
        entity.teacherUserId = group.teacherUserId();
        entity.name = group.name();
        entity.monthlyFeeCents = group.monthlyFeeCents();
        entity.createdAt = group.createdAt();
        return entity;
    }

    FixedGroup toDomain() {
        return new FixedGroup(id, teacherUserId, name, monthlyFeeCents, createdAt);
    }
}

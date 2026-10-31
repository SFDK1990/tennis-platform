package com.tennisplatform.membership.adapters.out.persistence;

import com.tennisplatform.membership.domain.FeeSettings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "fee_settings")
class FeeSettingsEntity {

    @Id
    @Column(name = "teacher_user_id")
    private UUID teacherUserId;

    @Column(name = "due_day", nullable = false)
    private int dueDay;

    protected FeeSettingsEntity() {
    }

    FeeSettingsEntity(UUID teacherUserId, FeeSettings settings) {
        this.teacherUserId = teacherUserId;
        this.dueDay = settings.dueDay();
    }

    FeeSettings toDomain() {
        return new FeeSettings(dueDay);
    }
}

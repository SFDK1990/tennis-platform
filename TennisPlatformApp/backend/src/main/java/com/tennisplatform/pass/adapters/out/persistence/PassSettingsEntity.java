package com.tennisplatform.pass.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "pass_settings")
class PassSettingsEntity {

    @Id
    @Column(name = "teacher_user_id")
    private UUID teacherUserId;

    @Column(nullable = false)
    private boolean required;

    protected PassSettingsEntity() {
    }

    PassSettingsEntity(UUID teacherUserId, boolean required) {
        this.teacherUserId = teacherUserId;
        this.required = required;
    }

    boolean required() {
        return required;
    }
}

package com.tennisplatform.pass.adapters.out.persistence;

import com.tennisplatform.pass.domain.PassSettings;
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

    @Column(name = "online_payment", nullable = false)
    private boolean onlinePayment;

    protected PassSettingsEntity() {
    }

    PassSettingsEntity(UUID teacherUserId, PassSettings settings) {
        this.teacherUserId = teacherUserId;
        this.required = settings.required();
        this.onlinePayment = settings.onlinePayment();
    }

    PassSettings toDomain() {
        return new PassSettings(required, onlinePayment);
    }
}

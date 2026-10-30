package com.tennisplatform.pass.adapters.out.persistence;

import com.tennisplatform.pass.application.port.out.PassSettingsRepository;
import com.tennisplatform.pass.domain.PassSettings;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
class PassSettingsRepositoryAdapter implements PassSettingsRepository {

    private final PassSettingsJpaRepository jpa;

    PassSettingsRepositoryAdapter(PassSettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PassSettings of(UUID teacherUserId) {
        return jpa.findById(teacherUserId).map(PassSettingsEntity::toDomain).orElse(PassSettings.DEFAULTS);
    }

    @Override
    public void save(UUID teacherUserId, PassSettings settings) {
        jpa.save(new PassSettingsEntity(teacherUserId, settings));
    }
}

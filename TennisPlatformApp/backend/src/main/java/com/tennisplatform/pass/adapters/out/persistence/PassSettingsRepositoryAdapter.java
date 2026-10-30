package com.tennisplatform.pass.adapters.out.persistence;

import com.tennisplatform.pass.application.port.out.PassSettingsRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
class PassSettingsRepositoryAdapter implements PassSettingsRepository {

    private final PassSettingsJpaRepository jpa;

    PassSettingsRepositoryAdapter(PassSettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean required(UUID teacherUserId) {
        return jpa.findById(teacherUserId).map(PassSettingsEntity::required).orElse(false);
    }

    @Override
    public void save(UUID teacherUserId, boolean required) {
        jpa.save(new PassSettingsEntity(teacherUserId, required));
    }
}

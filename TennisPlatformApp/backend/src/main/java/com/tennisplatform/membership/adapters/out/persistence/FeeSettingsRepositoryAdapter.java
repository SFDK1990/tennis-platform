package com.tennisplatform.membership.adapters.out.persistence;

import com.tennisplatform.membership.application.port.out.FeeSettingsRepository;
import com.tennisplatform.membership.domain.FeeSettings;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
class FeeSettingsRepositoryAdapter implements FeeSettingsRepository {

    private final FeeSettingsJpaRepository jpa;

    FeeSettingsRepositoryAdapter(FeeSettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public FeeSettings of(UUID teacherUserId) {
        return jpa.findById(teacherUserId).map(FeeSettingsEntity::toDomain).orElse(FeeSettings.DEFAULT);
    }

    @Override
    public void save(UUID teacherUserId, FeeSettings settings) {
        jpa.save(new FeeSettingsEntity(teacherUserId, settings));
    }
}

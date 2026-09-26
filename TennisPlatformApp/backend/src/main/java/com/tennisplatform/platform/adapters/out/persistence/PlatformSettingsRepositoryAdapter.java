package com.tennisplatform.platform.adapters.out.persistence;

import com.tennisplatform.platform.application.port.out.PlatformSettingsRepository;
import com.tennisplatform.platform.domain.PlatformSettings;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class PlatformSettingsRepositoryAdapter implements PlatformSettingsRepository {

    /** The id of the singleton row, fixed by {@code CHECK (id = 1)} in the schema. */
    private static final short SINGLETON_ID = 1;

    private final PlatformConfigurationJpaRepository jpa;

    PlatformSettingsRepositoryAdapter(PlatformConfigurationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PlatformSettings> find() {
        return jpa.findById(SINGLETON_ID).map(PlatformConfigurationEntity::toDomain);
    }

    @Override
    public PlatformSettings save(PlatformSettings settings) {
        PlatformConfigurationEntity entity = jpa.findById(SINGLETON_ID).orElseThrow(() ->
                new IllegalStateException("The platform configuration row is missing"));
        entity.update(settings);
        return jpa.save(entity).toDomain();
    }
}

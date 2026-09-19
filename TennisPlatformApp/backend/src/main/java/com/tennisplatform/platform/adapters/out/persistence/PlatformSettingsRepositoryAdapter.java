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
}

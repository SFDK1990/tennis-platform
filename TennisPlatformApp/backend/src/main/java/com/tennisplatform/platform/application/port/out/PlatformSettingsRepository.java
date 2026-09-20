package com.tennisplatform.platform.application.port.out;

import com.tennisplatform.platform.domain.PlatformSettings;

import java.util.Optional;

public interface PlatformSettingsRepository {

    /**
     * The single configuration row, empty only if it was deleted by hand: the Liquibase
     * changeset seeds it, so a running installation always has one.
     */
    Optional<PlatformSettings> find();
}

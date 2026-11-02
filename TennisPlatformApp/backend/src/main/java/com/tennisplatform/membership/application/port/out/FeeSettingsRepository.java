package com.tennisplatform.membership.application.port.out;

import com.tennisplatform.membership.domain.FeeSettings;

import java.util.UUID;

public interface FeeSettingsRepository {

    /** The teacher's settings, or {@link FeeSettings#DEFAULT} until they change them. */
    FeeSettings of(UUID teacherUserId);

    void save(UUID teacherUserId, FeeSettings settings);
}

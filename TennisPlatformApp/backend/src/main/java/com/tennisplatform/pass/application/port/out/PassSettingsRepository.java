package com.tennisplatform.pass.application.port.out;

import com.tennisplatform.pass.domain.PassSettings;
import java.util.UUID;

/** The teacher's switches. A teacher who never touched them has both off. */
public interface PassSettingsRepository {

    PassSettings of(UUID teacherUserId);

    void save(UUID teacherUserId, PassSettings settings);
}

package com.tennisplatform.pass.application.port.out;

import java.util.UUID;

/** Whether the teacher requires a pass to book. A teacher who never said is not requiring one. */
public interface PassSettingsRepository {

    boolean required(UUID teacherUserId);

    void save(UUID teacherUserId, boolean required);
}

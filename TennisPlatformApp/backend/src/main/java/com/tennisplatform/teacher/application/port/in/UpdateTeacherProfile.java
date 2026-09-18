package com.tennisplatform.teacher.application.port.in;

import java.util.UUID;

public interface UpdateTeacherProfile {

    /**
     * Applies a partial update. The caller id comes from the verified token, and the service
     * refuses to update a profile that does not belong to it: authorization by ownership, not
     * only by role, as required by 08-security-engineer.md.
     *
     * <p>A null field means "leave it as it is"; an empty phone clears it.
     */
    TeacherProfileView update(UUID callerId, TeacherProfileUpdate update);
}

package com.tennisplatform.student.application.port.in;

import java.util.UUID;

public interface UpdateStudentProfile {

    /**
     * Applies a partial update, creating the profile the first time. The caller id comes from
     * the verified token, so a student can only ever write their own row - ownership is not
     * something the request gets to claim.
     *
     * <p>The first call has to carry {@code fullName}, because the column is NOT NULL and a
     * profile without a name could not be stored; later calls may be partial.
     */
    StudentProfileView update(UUID callerId, StudentProfileUpdate update);
}

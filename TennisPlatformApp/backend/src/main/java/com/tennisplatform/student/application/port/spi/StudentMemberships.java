package com.tennisplatform.student.application.port.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * What letting a student go, disabling their account or deleting it needs done to their fixed
 * groups, which only {@code membership} can do (Fase 28). Runs inside the caller's transaction,
 * next to {@link StudentBookings}, which already cancels the seats.
 */
public interface StudentMemberships {

    /** Ends at once every membership of the student in this teacher's groups: no more months are owed. */
    void endAllWith(UUID teacherUserId, UUID studentUserId, Instant at);
}

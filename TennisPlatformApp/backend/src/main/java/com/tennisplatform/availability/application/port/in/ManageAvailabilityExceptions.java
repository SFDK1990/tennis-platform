package com.tennisplatform.availability.application.port.in;

import java.util.UUID;

public interface ManageAvailabilityExceptions {

    /**
     * Adds a block or an extra to one date.
     *
     * <p>Overlapping exceptions are accepted rather than rejected: two blocks that cover the
     * same hours block the same hours, and two extras that do add the same hours. The operation
     * is idempotent in effect, and refusing it would be ceremony.
     */
    AvailabilityOverrideView add(UUID teacherUserId, NewAvailabilityOverride exception);

    /** Removes one. Answers the same way whether the id never existed or belongs to someone else. */
    void remove(UUID teacherUserId, UUID exceptionId);
}

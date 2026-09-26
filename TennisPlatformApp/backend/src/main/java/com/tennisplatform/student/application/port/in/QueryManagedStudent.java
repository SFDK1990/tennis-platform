package com.tennisplatform.student.application.port.in;

import java.util.UUID;

/**
 * Whether a teacher currently manages a student - the one question {@code booking} has to ask
 * before letting anybody take a seat.
 *
 * <p>A yes or no on purpose, rather than a view: the caller needs the answer and nothing else,
 * and a port that returned the student's record would hand personal data to a module that has
 * no use for it.
 */
public interface QueryManagedStudent {

    /** True only while the relationship exists and is active; a deactivated student answers false. */
    boolean isManagedBy(UUID teacherUserId, UUID studentUserId);
}

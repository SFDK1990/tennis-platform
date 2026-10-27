package com.tennisplatform.student.application.port.in;

import java.util.List;
import java.util.Optional;
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

    /** The level the teacher gave the student (Fase 23); empty when they have none. */
    Optional<UUID> levelOf(UUID teacherUserId, UUID studentUserId);

    /** What each teacher wrote about the student, for the student's own export (Fase 23, decision 7). */
    List<StudentRecordView> recordsOf(UUID studentUserId);

    /** The teachers who currently manage this student; empty for a student nobody manages. */
    List<UUID> teachersOf(UUID studentUserId);

    /** The students this teacher currently manages, whom a message to everybody reaches (Fase 24). */
    List<UUID> studentsOf(UUID teacherUserId);

    /** The students this teacher currently manages and has put on this level (Fase 24). */
    List<UUID> studentsOn(UUID teacherUserId, UUID levelId);
}

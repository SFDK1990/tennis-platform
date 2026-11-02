package com.tennisplatform.student.application.port.in;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** The objectives the teacher marks as achieved for each student (44-fase29-analisis-progreso.md). */
public interface StudentProgress {

    /**
     * Every level, for the teacher's record of the student. Readable while there is a relationship,
     * like the record itself.
     */
    StudentProgressView ofManagedStudent(UUID teacherUserId, UUID studentUserId);

    /**
     * Marks it as achieved on {@code achievedOn}, today in the teacher's zone when null, and tells the
     * student the first time. Marking it again only changes the day.
     */
    StudentProgressView mark(UUID teacherUserId, UUID studentUserId, UUID objectiveId, LocalDate achievedOn);

    /** Leaves no trace; unmarking one that was not marked does nothing. */
    void unmark(UUID teacherUserId, UUID studentUserId, UUID objectiveId);

    /** What the student sees: their current level and those where they achieved something. */
    StudentProgressView ofStudent(UUID studentUserId);

    /** Which of these students have achieved every live objective of their current level. */
    Set<UUID> withLevelCompleted(UUID teacherUserId, Collection<UUID> studentUserIds);
}

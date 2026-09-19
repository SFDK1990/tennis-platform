package com.tennisplatform.student.application.port.out;

import com.tennisplatform.student.domain.ManagedStudent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManagedStudentRepository {

    /**
     * The relationship between these two, whatever its status. Asking without filtering by
     * status is what lets the use case tell "already managed" (409) from "deactivated, can be
     * taken back" - a query for MANAGED rows only would make the second look like the absence
     * of a relationship and create a duplicate.
     */
    Optional<ManagedStudent> findByPair(UUID teacherUserId, UUID studentUserId);

    /** Every relationship of this teacher, active or not, ordered oldest first. */
    List<ManagedStudent> findAllByTeacher(UUID teacherUserId);

    /** How many students this teacher currently manages, for the platform limit. */
    long countManagedBy(UUID teacherUserId);

    ManagedStudent save(ManagedStudent relationship);
}

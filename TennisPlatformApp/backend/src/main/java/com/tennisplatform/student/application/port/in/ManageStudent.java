package com.tennisplatform.student.application.port.in;

import java.util.UUID;

public interface ManageStudent {

    /**
     * Takes a student under management, or takes back one who was deactivated.
     *
     * <p>Reactivation updates the existing row rather than adding a second one: the unique
     * index on the pair makes a duplicate impossible in the schema, and this is the use case
     * that makes it impossible without relying on a constraint violation to find out.
     *
     * <p>Refuses when the limit in the platform configuration is already reached, and when the
     * student has not filled their personal data in yet.
     */
    ManagedStudentView manage(UUID teacherUserId, UUID studentUserId);

    /**
     * Stops managing a student: the relationship goes INACTIVE and the moment is recorded. The
     * row survives, so the student can be taken back later without a second relationship.
     *
     * <p>Their upcoming bookings with this teacher are cancelled in the same transaction, as
     * 01-analisis-funcional.md requires. Bookings of lessons that already started are kept.
     */
    void stopManaging(UUID teacherUserId, UUID studentUserId);
}

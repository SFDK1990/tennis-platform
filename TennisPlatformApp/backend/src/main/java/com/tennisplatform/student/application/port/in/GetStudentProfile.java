package com.tennisplatform.student.application.port.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Public query side of the module. Other modules use this - never the repository, never the JPA
 * entity - to read a student's personal data.
 */
public interface GetStudentProfile {

    /**
     * The profile of a user, empty when they have not filled their data in yet or are not a
     * student. The emptiness is meaningful and is not an error: it is what tells {@code /me}
     * to answer with nulls so the frontend knows it has to ask for the data.
     */
    Optional<StudentProfileView> byUserId(UUID userId);
}

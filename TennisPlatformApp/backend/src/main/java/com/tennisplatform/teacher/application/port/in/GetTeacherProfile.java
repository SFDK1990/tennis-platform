package com.tennisplatform.teacher.application.port.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Public query side of the module. Other modules use this - never the repository, never the JPA
 * entity - to read the teacher's profile.
 */
public interface GetTeacherProfile {

    /** The single teacher's profile. Throws if the bootstrap has not created it. */
    TeacherProfileView get();

    /**
     * The profile of a specific user, empty when that user is not the teacher. This is what lets
     * {@code /me} be composed without the caller having to know which module owns which field.
     */
    Optional<TeacherProfileView> byUserId(UUID userId);
}

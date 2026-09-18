package com.tennisplatform.teacher.application.port.out;

import com.tennisplatform.teacher.domain.TeacherProfile;

import java.util.Optional;
import java.util.UUID;

public interface TeacherProfileRepository {

    /**
     * The profile of the single teacher, without needing to know their id. The MVP has exactly
     * one teacher - enforced by a partial unique index on {@code users} - which is what makes a
     * lookup with no argument meaningful. It is also the seam to revisit if the platform ever
     * holds more than one.
     */
    Optional<TeacherProfile> findTheTeacher();

    Optional<TeacherProfile> findByUserId(UUID userId);

    TeacherProfile save(TeacherProfile profile);
}

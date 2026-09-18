package com.tennisplatform.teacher.application.port.in;

import com.tennisplatform.teacher.domain.TeacherProfile;

import java.util.UUID;

/**
 * What the teacher profile looks like to anything outside this module, including other modules.
 * The domain object never leaves: exposing it would let a caller mutate the profile through the
 * methods that enforce its rules.
 *
 * <p>The time zone travels as a string because that is what it is on the wire (an IANA id), and
 * a caller that only forwards it should not have to depend on {@code java.time} semantics.
 */
public record TeacherProfileView(UUID userId, String displayName, String phone, String timezone) {

    public static TeacherProfileView from(TeacherProfile profile) {
        return new TeacherProfileView(profile.userId(), profile.displayName(), profile.phone(),
                profile.timezone().getId());
    }
}

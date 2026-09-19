package com.tennisplatform.student.application.port.in;

import com.tennisplatform.student.domain.StudentProfile;

import java.util.UUID;

/**
 * What a student profile looks like to anything outside this module. The domain object never
 * leaves: handing it over would let a caller mutate the profile through the very methods that
 * enforce its rules.
 *
 * <p>It carries {@code nationalId} and {@code address}, so whoever builds a response out of it
 * is responsible for only doing so for the student themselves, the teacher managing them or an
 * ADMIN (08-security-engineer.md). The listing views exist precisely so that no caller has to
 * hold this one just to show a name.
 */
public record StudentProfileView(UUID userId, String fullName, String phone, String nationalId,
                                 String address) {

    public static StudentProfileView from(StudentProfile profile) {
        return new StudentProfileView(profile.userId(), profile.fullName(), profile.phone(),
                profile.nationalId(), profile.address());
    }
}

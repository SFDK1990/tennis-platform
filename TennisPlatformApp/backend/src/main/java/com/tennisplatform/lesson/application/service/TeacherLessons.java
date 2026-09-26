package com.tennisplatform.lesson.application.service;

import com.tennisplatform.lesson.domain.TeacherRoleRequiredException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;

import java.time.ZoneId;
import java.util.UUID;

/**
 * The check every operation in this module makes: that the caller is the teacher whose diary is
 * being read or written.
 *
 * <p>It reads through {@code teacher}'s inbound port, the only thing the boundary rules let this
 * module see of it, and returns the zone that comes back with the profile. Unlike the equivalent
 * in {@code availability}, the zone is genuinely used here: deciding whether a lesson runs past
 * midnight is a local question, and a day only means something in somebody's zone.
 *
 * <p>The zone is not copied into this module's tables. Two copies of a time zone drift, and the
 * day they disagree every lesson moves by an hour without anything failing.
 */
class TeacherLessons {

    private final GetTeacherProfile teacherProfile;

    TeacherLessons(GetTeacherProfile teacherProfile) {
        this.teacherProfile = teacherProfile;
    }

    /**
     * Checks ownership, not only the role the controller already checked.
     *
     * <p>Looking the caller up among teacher profiles is what makes this hold: the role in a
     * token says what kind of account it is, this says it is the account that owns what is being
     * touched. 08-security-engineer.md requires both, and neither replaces the other. With a
     * single teacher it looks redundant; it is not, and the day there are two, a check that is
     * missing does not fail - it hits the wrong diary.
     */
    ZoneId requireTheTeacher(UUID callerId) {
        TeacherProfileView profile = teacherProfile.byUserId(callerId)
                .orElseThrow(() -> new TeacherRoleRequiredException(
                        "Only the teacher can manage lessons"));
        return ZoneId.of(profile.timezone());
    }
}

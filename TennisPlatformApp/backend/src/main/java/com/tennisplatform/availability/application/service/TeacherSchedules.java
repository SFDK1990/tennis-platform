package com.tennisplatform.availability.application.service;

import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;

import java.util.UUID;

/**
 * The check every write in this module has to make: that the caller is the teacher who owns the
 * schedule being changed.
 *
 * <p>It reads through {@code teacher}'s inbound port, the only thing the boundary rules let this
 * module see of it. The time zone is read from that same profile wherever it is needed rather
 * than being copied into this module's tables: two copies of a time zone drift, and the day they
 * disagree the whole schedule moves by an hour without anything failing.
 */
class TeacherSchedules {

    private final GetTeacherProfile teacherProfile;

    TeacherSchedules(GetTeacherProfile teacherProfile) {
        this.teacherProfile = teacherProfile;
    }

    /**
     * Checks ownership, not only the role the controller already checked.
     *
     * <p>Looking the caller up among teacher profiles is what makes this hold: the role in a
     * token says what kind of account it is, this says it is the account that owns what is being
     * changed. 08-security-engineer.md requires both, and neither replaces the other.
     */
    void requireTheTeacher(UUID callerId) {
        if (teacherProfile.byUserId(callerId).isEmpty()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher can change the availability");
        }
    }
}

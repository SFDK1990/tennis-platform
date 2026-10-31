package com.tennisplatform.membership.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Signing up to a fixed group and leaving it, by the student or by the teacher (decision 2).
 *
 * <p>Joining books a seat in every lesson of the group still to come, all or none. Leaving keeps
 * the lessons of the month under way and frees the rest (decision 5).
 */
public interface JoinGroups {

    MembershipView join(UUID studentUserId, boolean emailVerified, UUID groupId);

    void leave(UUID studentUserId, UUID groupId);

    /** The teacher signs a student up; the email does not have to be verified, they know them. */
    GroupView.MemberView add(UUID teacherUserId, UUID groupId, UUID studentUserId);

    void remove(UUID teacherUserId, UUID groupId, UUID studentUserId);

    /** The student's memberships, those that ended included, the newest first. */
    List<MembershipView> ofStudent(UUID studentUserId);
}

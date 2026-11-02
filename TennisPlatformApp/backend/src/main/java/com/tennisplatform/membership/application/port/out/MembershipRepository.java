package com.tennisplatform.membership.application.port.out;

import com.tennisplatform.membership.domain.GroupMembership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository {

    /** Translates a second open membership of the student in the group into {@code AlreadyInGroupException}. */
    GroupMembership save(GroupMembership membership);

    Optional<GroupMembership> findById(UUID id);

    /** The memberships of the student in the group, the newest first. */
    List<GroupMembership> findOfStudentInGroup(UUID groupId, UUID studentUserId);

    List<GroupMembership> findByGroup(UUID groupId);

    List<GroupMembership> findByTeacher(UUID teacherUserId);

    List<GroupMembership> findByStudent(UUID studentUserId);

    boolean existsInGroup(UUID groupId);
}

package com.tennisplatform.membership.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface GroupMembershipJpaRepository extends JpaRepository<GroupMembershipEntity, UUID> {

    List<GroupMembershipEntity> findAllByGroupIdAndStudentUserIdOrderByJoinedAtDesc(UUID groupId, UUID studentUserId);

    List<GroupMembershipEntity> findAllByGroupIdOrderByJoinedAtAsc(UUID groupId);

    List<GroupMembershipEntity> findAllByTeacherUserIdOrderByJoinedAtAsc(UUID teacherUserId);

    List<GroupMembershipEntity> findAllByStudentUserIdOrderByJoinedAtDesc(UUID studentUserId);

    boolean existsByGroupId(UUID groupId);
}

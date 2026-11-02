package com.tennisplatform.membership.adapters.out.persistence;

import com.tennisplatform.membership.domain.GroupMembership;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_memberships")
class GroupMembershipEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Column(name = "student_user_id", nullable = false, updatable = false)
    private UUID studentUserId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected GroupMembershipEntity() {
    }

    static GroupMembershipEntity fromDomain(GroupMembership membership) {
        GroupMembershipEntity entity = new GroupMembershipEntity();
        entity.id = membership.id();
        entity.groupId = membership.groupId();
        entity.teacherUserId = membership.teacherUserId();
        entity.studentUserId = membership.studentUserId();
        entity.joinedAt = membership.joinedAt();
        entity.leftAt = membership.leftAt();
        return entity;
    }

    GroupMembership toDomain() {
        return new GroupMembership(id, groupId, teacherUserId, studentUserId, joinedAt, leftAt);
    }
}

package com.tennisplatform.membership.adapters.out.persistence;

import com.tennisplatform.membership.application.port.out.MembershipRepository;
import com.tennisplatform.membership.domain.AlreadyInGroupException;
import com.tennisplatform.membership.domain.GroupMembership;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class MembershipRepositoryAdapter implements MembershipRepository {

    private final GroupMembershipJpaRepository jpa;

    MembershipRepositoryAdapter(GroupMembershipJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public GroupMembership save(GroupMembership membership) {
        try {
            return jpa.saveAndFlush(GroupMembershipEntity.fromDomain(membership)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.is(e, ConstraintViolations.UNIQUE_VIOLATION)) {
                throw new AlreadyInGroupException("The student is already in this group");
            }
            throw e;
        }
    }

    @Override
    public Optional<GroupMembership> findById(UUID id) {
        return jpa.findById(id).map(GroupMembershipEntity::toDomain);
    }

    @Override
    public List<GroupMembership> findOfStudentInGroup(UUID groupId, UUID studentUserId) {
        return jpa.findAllByGroupIdAndStudentUserIdOrderByJoinedAtDesc(groupId, studentUserId).stream()
                .map(GroupMembershipEntity::toDomain).toList();
    }

    @Override
    public List<GroupMembership> findByGroup(UUID groupId) {
        return jpa.findAllByGroupIdOrderByJoinedAtAsc(groupId).stream().map(GroupMembershipEntity::toDomain).toList();
    }

    @Override
    public List<GroupMembership> findByTeacher(UUID teacherUserId) {
        return jpa.findAllByTeacherUserIdOrderByJoinedAtAsc(teacherUserId).stream()
                .map(GroupMembershipEntity::toDomain).toList();
    }

    @Override
    public List<GroupMembership> findByStudent(UUID studentUserId) {
        return jpa.findAllByStudentUserIdOrderByJoinedAtDesc(studentUserId).stream()
                .map(GroupMembershipEntity::toDomain).toList();
    }

    @Override
    public boolean existsInGroup(UUID groupId) {
        return jpa.existsByGroupId(groupId);
    }
}

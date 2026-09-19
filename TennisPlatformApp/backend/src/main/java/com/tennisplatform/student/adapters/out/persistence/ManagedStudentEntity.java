package com.tennisplatform.student.adapters.out.persistence;

import com.tennisplatform.student.domain.ManagedStatus;
import com.tennisplatform.student.domain.ManagedStudent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teacher_students")
class ManagedStudentEntity {

    @Id
    private UUID id;

    @Column(name = "teacher_user_id", nullable = false)
    private UUID teacherUserId;

    @Column(name = "student_user_id", nullable = false)
    private UUID studentUserId;

    /**
     * Stored as its name, matching the CHECK constraint in the schema. Ordinals would make the
     * column depend on the declaration order of the enum, so inserting a value in the middle
     * would silently rewrite the meaning of every existing row.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ManagedStatus status;

    @Column(name = "managed_at", nullable = false)
    private Instant managedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected ManagedStudentEntity() {
    }

    static ManagedStudentEntity fromDomain(ManagedStudent relationship) {
        ManagedStudentEntity entity = new ManagedStudentEntity();
        entity.id = relationship.id();
        entity.teacherUserId = relationship.teacherUserId();
        entity.studentUserId = relationship.studentUserId();
        entity.status = relationship.status();
        entity.managedAt = relationship.managedAt();
        entity.deactivatedAt = relationship.deactivatedAt();
        return entity;
    }

    ManagedStudent toDomain() {
        return ManagedStudent.rehydrate(id, teacherUserId, studentUserId, status, managedAt,
                deactivatedAt);
    }
}

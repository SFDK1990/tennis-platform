package com.tennisplatform.booking.adapters.out.persistence;

import com.tennisplatform.booking.domain.WaitingListEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "waiting_list_entries")
class WaitingListEntryEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "lesson_id", nullable = false, updatable = false)
    private UUID lessonId;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Column(name = "student_user_id", nullable = false, updatable = false)
    private UUID studentUserId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected WaitingListEntryEntity() {
    }

    static WaitingListEntryEntity fromDomain(WaitingListEntry entry) {
        WaitingListEntryEntity entity = new WaitingListEntryEntity();
        entity.id = entry.id();
        entity.lessonId = entry.lessonId();
        entity.teacherUserId = entry.teacherUserId();
        entity.studentUserId = entry.studentUserId();
        entity.joinedAt = entry.joinedAt();
        return entity;
    }

    WaitingListEntry toDomain() {
        return new WaitingListEntry(id, lessonId, teacherUserId, studentUserId, joinedAt);
    }
}

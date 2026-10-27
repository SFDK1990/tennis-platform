package com.tennisplatform.student.adapters.out.persistence;

import com.tennisplatform.student.domain.StudentPhoto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_photos")
class StudentPhotoEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentPhotoEntity() {
    }

    static StudentPhotoEntity fromDomain(StudentPhoto photo, Instant at) {
        StudentPhotoEntity entity = new StudentPhotoEntity();
        entity.userId = photo.studentUserId();
        entity.contentType = photo.contentType();
        entity.data = photo.bytes();
        entity.updatedAt = at;
        return entity;
    }

    StudentPhoto toDomain() {
        return StudentPhoto.rehydrate(userId, contentType, data);
    }
}

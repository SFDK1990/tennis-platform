package com.tennisplatform.student.adapters.out.persistence;

import com.tennisplatform.student.domain.StudentProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_profiles")
class StudentProfileEntity {

    /** The account id: a student has exactly one profile, so there is no surrogate key. */
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column
    private String phone;

    @Column(name = "national_id")
    private String nationalId;

    @Column
    private String address;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentProfileEntity() {
    }

    static StudentProfileEntity fromDomain(StudentProfile profile) {
        StudentProfileEntity entity = new StudentProfileEntity();
        entity.userId = profile.userId();
        entity.fullName = profile.fullName();
        entity.phone = profile.phone();
        entity.nationalId = profile.nationalId();
        entity.address = profile.address();
        entity.createdAt = profile.createdAt();
        return entity;
    }

    StudentProfile toDomain() {
        return StudentProfile.rehydrate(userId, fullName, phone, nationalId, address, createdAt);
    }
}

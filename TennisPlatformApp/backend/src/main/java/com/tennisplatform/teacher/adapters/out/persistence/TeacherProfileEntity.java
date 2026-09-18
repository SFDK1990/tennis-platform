package com.tennisplatform.teacher.adapters.out.persistence;

import com.tennisplatform.teacher.domain.TeacherProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "teacher_profiles")
class TeacherProfileEntity {

    /** The account id: a teacher has exactly one profile, so there is no surrogate key. */
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column
    private String phone;

    /** Stored as the IANA id. Parsing back is safe: nothing else writes this column. */
    @Column(nullable = false)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TeacherProfileEntity() {
    }

    static TeacherProfileEntity fromDomain(TeacherProfile profile) {
        TeacherProfileEntity entity = new TeacherProfileEntity();
        entity.userId = profile.userId();
        entity.displayName = profile.displayName();
        entity.phone = profile.phone();
        entity.timezone = profile.timezone().getId();
        entity.createdAt = profile.createdAt();
        return entity;
    }

    TeacherProfile toDomain() {
        return TeacherProfile.rehydrate(userId, displayName, phone, ZoneId.of(timezone), createdAt);
    }
}

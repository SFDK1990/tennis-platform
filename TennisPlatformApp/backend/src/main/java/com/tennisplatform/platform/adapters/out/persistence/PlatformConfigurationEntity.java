package com.tennisplatform.platform.adapters.out.persistence;

import com.tennisplatform.platform.domain.PlatformSettings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_configuration")
class PlatformConfigurationEntity {

    /** Always 1: the schema rejects any other value, so the configuration stays a singleton. */
    @Id
    private Short id;

    @Column(name = "student_limit", nullable = false)
    private Integer studentLimit;

    @Column(name = "max_group_capacity", nullable = false)
    private Integer maxGroupCapacity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Who last changed the configuration, written by the Fase 7 console. Mapped as a plain UUID
     * and not as a relation: in the database it is a foreign key to {@code users}, because the
     * integrity of an audit field is worth keeping, but this module does not depend on
     * {@code identity} and must not learn what a user is to read a number.
     */
    @Column(name = "updated_by")
    private UUID updatedBy;

    protected PlatformConfigurationEntity() {
    }

    PlatformSettings toDomain() {
        return PlatformSettings.of(studentLimit, maxGroupCapacity);
    }
}

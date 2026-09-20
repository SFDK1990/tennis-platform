package com.tennisplatform.availability.adapters.out.persistence;

import com.tennisplatform.availability.domain.AvailabilityOverrideType;
import com.tennisplatform.availability.domain.AvailabilityOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "availability_exceptions")
class AvailabilityOverrideEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "teacher_user_id", nullable = false)
    private UUID teacherUserId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** Null on a block means the whole day. An extra always has both. */
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /** STRING, not ORDINAL: the column has a CHECK on the literals and reordering the enum must not silently remap existing rows. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private AvailabilityOverrideType type;

    protected AvailabilityOverrideEntity() {
    }

    static AvailabilityOverrideEntity fromDomain(AvailabilityOverride exception) {
        AvailabilityOverrideEntity entity = new AvailabilityOverrideEntity();
        entity.id = exception.id();
        entity.teacherUserId = exception.teacherUserId();
        entity.date = exception.date();
        entity.startTime = exception.startTime();
        entity.endTime = exception.endTime();
        entity.type = exception.type();
        return entity;
    }

    AvailabilityOverride toDomain() {
        return AvailabilityOverride.rehydrate(id, teacherUserId, date, startTime, endTime, type);
    }
}

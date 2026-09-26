package com.tennisplatform.platform.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * The global configuration of the installation. A singleton by design: there is one platform,
 * so there is one row and one object, and the schema enforces it with {@code CHECK (id = 1)}.
 *
 * <p>It is a domain object rather than a bare pair of integers because the values have rules -
 * both must be positive - and rules that live only in the column definition are rules the
 * application learns about from a constraint violation, too late to say anything useful.
 *
 * <p>{@code updatedBy} is null until somebody changes the configuration: the seeded row was
 * written by a changeset, not by a person.
 */
public final class PlatformSettings {

    private final int studentLimit;
    private final int maxGroupCapacity;
    private final Instant updatedAt;
    private final UUID updatedBy;

    private PlatformSettings(int studentLimit, int maxGroupCapacity, Instant updatedAt, UUID updatedBy) {
        if (studentLimit <= 0) {
            throw new InvalidPlatformSettingsException("The student limit must be positive");
        }
        if (maxGroupCapacity <= 0) {
            throw new InvalidPlatformSettingsException("The group capacity limit must be positive");
        }
        this.studentLimit = studentLimit;
        this.maxGroupCapacity = maxGroupCapacity;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static PlatformSettings of(int studentLimit, int maxGroupCapacity) {
        return new PlatformSettings(studentLimit, maxGroupCapacity, null, null);
    }

    public static PlatformSettings rehydrate(int studentLimit, int maxGroupCapacity, Instant updatedAt,
                                             UUID updatedBy) {
        return new PlatformSettings(studentLimit, maxGroupCapacity, updatedAt, updatedBy);
    }

    /**
     * The configuration after an administrator's change. A null value keeps the current one.
     *
     * <p>Lowering the student limit below the students already managed is allowed: nobody is
     * let go, the teacher just cannot take anybody on until they are under it again
     * (23-fase12-analisis-administracion.md). The same goes for the group capacity and the
     * lessons that already exist.
     */
    public PlatformSettings changedBy(UUID administrator, Instant at, Integer newStudentLimit,
                                      Integer newMaxGroupCapacity) {
        return new PlatformSettings(
                newStudentLimit == null ? studentLimit : newStudentLimit,
                newMaxGroupCapacity == null ? maxGroupCapacity : newMaxGroupCapacity,
                at, administrator);
    }

    public int studentLimit() {
        return studentLimit;
    }

    /**
     * The largest a group lesson may be.
     *
     * <p>Configurable rather than a constant because the right number depends on the court, and
     * the day it is wrong it is wrong for every lesson at once.
     */
    public int maxGroupCapacity() {
        return maxGroupCapacity;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public UUID updatedBy() {
        return updatedBy;
    }
}

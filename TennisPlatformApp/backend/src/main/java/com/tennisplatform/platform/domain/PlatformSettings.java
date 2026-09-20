package com.tennisplatform.platform.domain;

/**
 * The global configuration of the installation. A singleton by design: there is one platform,
 * so there is one row and one object, and the schema enforces it with {@code CHECK (id = 1)}.
 *
 * <p>It is a domain object rather than a bare pair of integers because the values have rules -
 * both must be positive - and rules that live only in the column definition are rules the
 * application learns about from a constraint violation, too late to say anything useful.
 */
public class PlatformSettings {

    private final int studentLimit;
    private final int maxGroupCapacity;

    private PlatformSettings(int studentLimit, int maxGroupCapacity) {
        this.studentLimit = studentLimit;
        this.maxGroupCapacity = maxGroupCapacity;
    }

    public static PlatformSettings of(int studentLimit, int maxGroupCapacity) {
        if (studentLimit <= 0) {
            throw new InvalidPlatformSettingsException("The student limit must be positive");
        }
        if (maxGroupCapacity <= 0) {
            throw new InvalidPlatformSettingsException("The group capacity limit must be positive");
        }
        return new PlatformSettings(studentLimit, maxGroupCapacity);
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
}

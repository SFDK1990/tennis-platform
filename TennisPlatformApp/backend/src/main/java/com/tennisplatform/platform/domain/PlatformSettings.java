package com.tennisplatform.platform.domain;

/**
 * The global configuration of the installation. A singleton by design: there is one platform,
 * so there is one row and one object, and the schema enforces it with {@code CHECK (id = 1)}.
 *
 * <p>It is a domain object rather than a bare integer because the limit has a rule - it must be
 * positive - and rules that live only in the column definition are rules the application learns
 * about from a constraint violation, too late to say anything useful.
 */
public class PlatformSettings {

    private final int studentLimit;

    private PlatformSettings(int studentLimit) {
        this.studentLimit = studentLimit;
    }

    public static PlatformSettings of(int studentLimit) {
        if (studentLimit <= 0) {
            throw new InvalidPlatformSettingsException("The student limit must be positive");
        }
        return new PlatformSettings(studentLimit);
    }

    public int studentLimit() {
        return studentLimit;
    }
}

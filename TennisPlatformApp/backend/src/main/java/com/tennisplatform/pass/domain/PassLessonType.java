package com.tennisplatform.pass.domain;

import java.util.Locale;

/**
 * Which lessons a pass pays for: group or private ones, which do not cost the same (decision 1).
 * The same wire values as a lesson's type, so a lesson and a pass are matched by their names.
 */
public enum PassLessonType {
    INDIVIDUAL,
    GROUP;

    public static PassLessonType parse(String value) {
        if (value == null) {
            throw new InvalidPassException("A pass type is either for INDIVIDUAL or for GROUP lessons");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidPassException("Unknown lesson type: " + value + ". Expected INDIVIDUAL or GROUP");
        }
    }
}

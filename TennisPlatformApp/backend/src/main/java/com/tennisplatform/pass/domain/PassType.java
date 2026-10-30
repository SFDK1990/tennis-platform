package com.tennisplatform.pass.domain;

import java.util.UUID;

/**
 * A kind of pass the teacher sells: "10 group lessons, 120 euros, three months"
 * (41-fase27-analisis-bonos.md, decisions 1 and 6).
 *
 * <p>The price is in cents, so no amount is ever a float. {@code validityMonths} is null for a
 * pass that never expires. A retired type is no longer sold, and the passes already sold of it
 * are untouched.
 */
public record PassType(UUID id, UUID teacherUserId, String name, PassLessonType lessonType, int lessons,
                       int priceCents, Integer validityMonths, boolean retired) {

    public static final int MAX_NAME = 60;
    public static final int MAX_LESSONS = 200;
    public static final int MAX_VALIDITY_MONTHS = 36;

    public PassType {
        if (name == null || name.isBlank()) {
            throw new InvalidPassException("A pass type needs a name");
        }
        name = name.trim();
        if (name.length() > MAX_NAME) {
            throw new InvalidPassException("A pass type's name has at most " + MAX_NAME + " characters");
        }
        if (lessonType == null) {
            throw new InvalidPassException("A pass type is either for INDIVIDUAL or for GROUP lessons");
        }
        if (lessons < 1 || lessons > MAX_LESSONS) {
            throw new InvalidPassException("A pass has between 1 and " + MAX_LESSONS + " lessons");
        }
        if (priceCents < 0) {
            throw new InvalidPassException("A price cannot be negative");
        }
        if (validityMonths != null && (validityMonths < 1 || validityMonths > MAX_VALIDITY_MONTHS)) {
            throw new InvalidPassException("A pass lasts between 1 and " + MAX_VALIDITY_MONTHS
                    + " months, or never expires");
        }
    }

    /**
     * The type after the teacher's change. A null keeps the current value, except for
     * {@code validityMonths}, where {@code clearValidity} says that the pass now never expires.
     */
    public PassType changed(String newName, String newLessonType, Integer newLessons, Integer newPriceCents,
                            Integer newValidityMonths, boolean clearValidity, Boolean newRetired) {
        return new PassType(id, teacherUserId,
                newName == null ? name : newName,
                newLessonType == null ? lessonType : PassLessonType.parse(newLessonType),
                newLessons == null ? lessons : newLessons,
                newPriceCents == null ? priceCents : newPriceCents,
                clearValidity ? null : newValidityMonths == null ? validityMonths : newValidityMonths,
                newRetired == null ? retired : newRetired);
    }
}

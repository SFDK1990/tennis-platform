package com.tennisplatform.lesson.domain;

/**
 * The lesson would run at the same time as another one of the teacher's.
 *
 * <p>Thrown both by the check the application makes before writing and by the translation of the
 * exclusion constraint the database enforces. The two are not redundant: the first exists so the
 * answer says something useful, the second because between the check and the INSERT another
 * request fits.
 */
public class LessonOverlapException extends RuntimeException {

    public LessonOverlapException(String message) {
        super(message);
    }
}

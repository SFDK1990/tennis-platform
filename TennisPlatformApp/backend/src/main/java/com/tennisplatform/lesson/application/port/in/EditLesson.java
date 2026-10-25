package com.tennisplatform.lesson.application.port.in;

import java.util.UUID;

/**
 * The teacher changes the notes or the capacity of a lesson that has not started
 * (30-fase19-analisis-cierre-mvp.md). The time stays: the students booked that time, and moving
 * it is cancelling and creating another.
 */
public interface EditLesson {

    /** Puts the lesson on a level, or opens it to everybody with null (Fase 23). Seats already taken stay. */
    LessonView changeLevel(UUID teacherUserId, UUID lessonId, UUID levelId);

    LessonView edit(UUID teacherUserId, UUID lessonId, Changes changes);

    /** Null leaves a value as it is; blank notes clear them. */
    record Changes(String notes, Integer capacity) {
    }
}

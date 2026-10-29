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

    /**
     * Moves a lesson that has not started to another court (Fase 25.6). The students with a seat
     * are told once the change is committed; moving it to the court it is on changes nothing.
     */
    LessonView changeCourt(UUID teacherUserId, UUID lessonId, UUID courtId);

    LessonView edit(UUID teacherUserId, UUID lessonId, Changes changes);

    /** Null leaves a value as it is; blank notes clear them. */
    record Changes(String notes, Integer capacity) {
    }
}

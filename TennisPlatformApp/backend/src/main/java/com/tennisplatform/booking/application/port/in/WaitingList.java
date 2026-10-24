package com.tennisplatform.booking.application.port.in;

import com.tennisplatform.lesson.application.port.in.LessonView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Waiting for a seat in a full lesson (34-fase21-analisis-lista-espera.md). */
public interface WaitingList {

    /**
     * Puts the student at the end of the lesson's list. The same checks as booking, in the same
     * order, and then: only for a full lesson, and only until 24 hours before it starts.
     */
    WaitingPlace join(UUID studentUserId, boolean emailVerified, UUID lessonId);

    /** Takes the student off the list. Leaving a list one is not on changes nothing. */
    void leave(UUID studentUserId, UUID lessonId);

    /** The lists the student is on that are still open, soonest lesson first. */
    List<WaitingPlace> forStudent(UUID studentUserId);

    /**
     * Who waits for a seat in one of the teacher's lessons, in order. Another teacher's lesson
     * answers an empty list, as listing its bookings does.
     */
    List<WaitingStudent> forTeacher(UUID teacherUserId, UUID lessonId);

    /** {@code position} counts from 1: the first is the next to get a seat. */
    record WaitingPlace(LessonView lesson, int position, Instant joinedAt) {
    }

    record WaitingStudent(UUID studentUserId, int position, Instant joinedAt) {
    }
}

package com.tennisplatform.lesson.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Cancels a lesson, freeing its slot.
 *
 * <p>The teacher may do this at any time, including with less than a day's notice. The
 * 24-hour window in 01-analisis-funcional.md protects the teacher from a gap they can no longer
 * fill, which is a reason to bind the student cancelling a booking and no reason at all to bind
 * the teacher cancelling their own lesson - a teacher who falls ill the night before has to be
 * able to. Whether it happened at short notice is recorded, so Fase 9 can tell who needs
 * warning.
 */
public interface CancelLesson {

    /** {@code reason} is what the teacher wrote, or null: it reaches the students and is not stored. */
    LessonView cancel(UUID teacherUserId, UUID lessonId, String reason);

    /**
     * Cancels every lesson of the teacher that starts on {@code day} in the teacher's zone and has
     * neither started nor been cancelled, each with its bookings, in one transaction: a rainy day
     * is cancelled whole or not at all (32-fase20-analisis-avisos-calendario.md).
     *
     * @return the lessons cancelled, empty when there were none
     */
    List<LessonView> cancelDay(UUID teacherUserId, LocalDate day, String reason);

    /**
     * An administrator resolves an incident (01-analisis-funcional.md §17): any lesson, with no
     * window either, and its bookings say the administration decided.
     */
    LessonView cancelAsAdmin(UUID lessonId);
}

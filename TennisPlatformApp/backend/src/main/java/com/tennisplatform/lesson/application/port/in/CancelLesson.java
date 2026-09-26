package com.tennisplatform.lesson.application.port.in;

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

    LessonView cancel(UUID teacherUserId, UUID lessonId);
}

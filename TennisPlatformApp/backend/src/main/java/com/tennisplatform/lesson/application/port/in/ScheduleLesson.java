package com.tennisplatform.lesson.application.port.in;

import java.util.UUID;

/** Creates a lesson for the teacher, validating it against their own hours and their own diary. */
public interface ScheduleLesson {

    /**
     * @param teacherUserId always taken from the authenticated token, never from the request body
     */
    LessonView schedule(UUID teacherUserId, NewLesson lesson);
}

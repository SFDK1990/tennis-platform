package com.tennisplatform.lesson.application.service;

import com.tennisplatform.lesson.domain.InvalidLessonException;
import com.tennisplatform.teacher.application.port.in.QueryLevels;

import java.util.UUID;

/** A lesson can only be put on one of its own teacher's levels (Fase 23). */
final class LessonLevels {

    private LessonLevels() {
    }

    static void requireTheTeachers(QueryLevels levels, UUID levelId, UUID teacherUserId) {
        if (levelId != null && !levels.belongsTo(levelId, teacherUserId)) {
            throw new InvalidLessonException("The level is not one of the teacher's");
        }
    }
}

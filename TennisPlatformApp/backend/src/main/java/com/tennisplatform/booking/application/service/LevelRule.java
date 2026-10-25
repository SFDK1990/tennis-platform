package com.tennisplatform.booking.application.service;

import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;

import java.util.Optional;
import java.util.UUID;

/**
 * Who may take a seat by level (36-fase23-analisis-niveles-ficha.md, decisions 4 and 5): a lesson
 * open to everybody, or one of the student's own level, never a level below or above. A student
 * the teacher has not given a level yet may book any lesson.
 */
final class LevelRule {

    private LevelRule() {
    }

    static boolean allows(QueryManagedStudent students, LessonView lesson, UUID studentUserId) {
        if (lesson.levelId() == null) {
            return true;
        }
        Optional<UUID> level = students.levelOf(lesson.teacherUserId(), studentUserId);
        return level.isEmpty() || level.get().equals(lesson.levelId());
    }
}

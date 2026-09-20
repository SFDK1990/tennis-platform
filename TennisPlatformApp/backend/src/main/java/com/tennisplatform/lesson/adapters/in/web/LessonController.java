package com.tennisplatform.lesson.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.LessonResponse;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A single lesson, readable by anyone authenticated.
 *
 * <p>The same split the teacher's profile and the availability already use: a student has to be
 * able to see what they would be signing up for, and a lesson carries nobody's personal data.
 * The one field that is the teacher's alone is {@code notes}, and this is the layer that knows
 * who is asking - so this is where it gets left out.
 */
@RestController
@RequestMapping("/api/v1/lessons")
class LessonController {

    private final GetLesson getLesson;

    LessonController(GetLesson getLesson) {
        this.getLesson = getLesson;
    }

    @GetMapping("/{id}")
    LessonResponse byId(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        LessonView lesson = getLesson.byId(id);
        return LessonResponse.from(lesson, ownsIt(caller, lesson));
    }

    /**
     * Role and ownership, not only role.
     *
     * <p>Everywhere else in this module the two are checked together, and the reason is written
     * out in {@code TeacherLessons}: the role says what kind of account is calling, ownership
     * says it is the account whose lesson this is. With a single teacher the two coincide, so a
     * role check alone would pass every test; the day there is a second teacher it would not
     * fail either - it would hand one teacher the other's notes.
     */
    private static boolean ownsIt(AuthenticatedUser caller, LessonView lesson) {
        return caller != null && caller.isTeacher() && caller.id().equals(lesson.teacherUserId());
    }
}

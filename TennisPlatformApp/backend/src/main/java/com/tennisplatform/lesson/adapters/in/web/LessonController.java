package com.tennisplatform.lesson.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.LessonResponse;
import com.tennisplatform.lesson.application.port.in.GetLesson;
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
        boolean forTheTeacher = caller != null && caller.isTeacher();
        return LessonResponse.from(getLesson.byId(id), forTheTeacher);
    }
}

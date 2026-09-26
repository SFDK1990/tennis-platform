package com.tennisplatform.lesson.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.CreateLessonRequest;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.LessonResponse;
import com.tennisplatform.lesson.application.port.in.CancelLesson;
import com.tennisplatform.lesson.application.port.in.NewLesson;
import com.tennisplatform.lesson.application.port.in.ScheduleLesson;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The teacher's own diary: everything here is written by the teacher and nobody else. The
 * teacher reads it through {@code /calendar}.
 *
 * <p>The role is checked here and the ownership in the service. Neither replaces the other: the
 * role says what kind of account is calling, ownership says it is the account that owns the
 * diary being touched.
 */
@RestController
@RequestMapping("/api/v1/teacher/lessons")
class TeacherLessonsController {

    private final ScheduleLesson scheduleLesson;
    private final CancelLesson cancelLesson;

    TeacherLessonsController(ScheduleLesson scheduleLesson, CancelLesson cancelLesson) {
        this.scheduleLesson = scheduleLesson;
        this.cancelLesson = cancelLesson;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    LessonResponse create(@AuthenticationPrincipal AuthenticatedUser caller,
                          @Valid @RequestBody CreateLessonRequest request) {
        requireTeacher(caller);
        return LessonResponse.from(scheduleLesson.schedule(caller.id(),
                new NewLesson(request.type(), request.startsAt(), request.endsAt(), request.capacity(),
                        request.notes(), request.overrideAvailability())), true);
    }

    @PostMapping("/{id}/cancel")
    LessonResponse cancel(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        return LessonResponse.from(cancelLesson.cancel(caller.id(), id), true);
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher can manage lessons");
        }
    }
}

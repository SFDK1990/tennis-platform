package com.tennisplatform.lesson.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.CreateLessonRequest;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.LessonResponse;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.LessonsResponse;
import com.tennisplatform.lesson.application.port.in.CancelLesson;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.NewLesson;
import com.tennisplatform.lesson.application.port.in.ScheduleLesson;
import com.tennisplatform.lesson.domain.TeacherRoleRequiredException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The teacher's own diary: everything here is written or read by the teacher and nobody else.
 *
 * <p>The role is checked here and the ownership in the service. Neither replaces the other: the
 * role says what kind of account is calling, ownership says it is the account that owns the
 * diary being touched.
 */
@RestController
@RequestMapping("/api/v1/teacher/lessons")
class TeacherLessonsController {

    private final ScheduleLesson scheduleLesson;
    private final GetLesson getLesson;
    private final CancelLesson cancelLesson;

    TeacherLessonsController(ScheduleLesson scheduleLesson, GetLesson getLesson, CancelLesson cancelLesson) {
        this.scheduleLesson = scheduleLesson;
        this.getLesson = getLesson;
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

    /**
     * The range is mandatory rather than defaulted, for the same reason the availability read
     * insists on one: a default would make the answer depend on today's date without the caller
     * knowing, and lessons accumulate for ever, so an unbounded listing gets slower every season.
     */
    @GetMapping
    LessonsResponse list(@AuthenticationPrincipal AuthenticatedUser caller,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        requireTeacher(caller);
        return LessonsResponse.of(getLesson.forTeacherBetween(caller.id(), from, to));
    }

    @PostMapping("/{id}/cancel")
    LessonResponse cancel(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        return LessonResponse.from(cancelLesson.cancel(caller.id(), id), true);
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw new TeacherRoleRequiredException("Only the teacher can manage lessons");
        }
    }
}

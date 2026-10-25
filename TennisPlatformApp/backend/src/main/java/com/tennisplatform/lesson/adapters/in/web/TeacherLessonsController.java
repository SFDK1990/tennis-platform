package com.tennisplatform.lesson.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.CancelDayRequest;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.CancelLessonRequest;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.CancelledDayResponse;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.CreateLessonRequest;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.EditLessonRequest;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.CreateSeriesRequest;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.LessonResponse;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.LessonsResponse;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.OccurrenceResponse;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.SeriesPreviewRequest;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.SeriesPreviewResponse;
import com.tennisplatform.lesson.application.port.in.CancelLesson;
import com.tennisplatform.lesson.application.port.in.EditLesson;
import com.tennisplatform.lesson.application.port.in.NewLesson;
import com.tennisplatform.lesson.application.port.in.ScheduleLesson;
import com.tennisplatform.lesson.application.port.in.ScheduleSeries;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final ScheduleSeries scheduleSeries;
    private final CancelLesson cancelLesson;
    private final EditLesson editLesson;

    TeacherLessonsController(ScheduleLesson scheduleLesson, ScheduleSeries scheduleSeries, CancelLesson cancelLesson,
                             EditLesson editLesson) {
        this.scheduleLesson = scheduleLesson;
        this.scheduleSeries = scheduleSeries;
        this.cancelLesson = cancelLesson;
        this.editLesson = editLesson;
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

    @PostMapping("/series/preview")
    SeriesPreviewResponse previewSeries(@AuthenticationPrincipal AuthenticatedUser caller,
                                        @Valid @RequestBody SeriesPreviewRequest request) {
        requireTeacher(caller);
        return new SeriesPreviewResponse(scheduleSeries.preview(caller.id(), new ScheduleSeries.NewSeries(
                        request.type(), request.startsAt(), request.endsAt(), request.capacity(), request.notes(),
                        request.weeks())).stream()
                .map(occurrence -> new OccurrenceResponse(occurrence.startsAt(), occurrence.endsAt(), occurrence.status()))
                .toList());
    }

    @PostMapping("/series")
    @ResponseStatus(HttpStatus.CREATED)
    LessonsResponse createSeries(@AuthenticationPrincipal AuthenticatedUser caller,
                                 @Valid @RequestBody CreateSeriesRequest request) {
        requireTeacher(caller);
        return new LessonsResponse(scheduleSeries.schedule(caller.id(), new ScheduleSeries.NewSeries(
                                request.type(), request.startsAt(), request.endsAt(), request.capacity(), request.notes(),
                                request.weeks()), request.dates(), request.overrideAvailability()).stream()
                .map(lesson -> LessonResponse.from(lesson, true))
                .toList());
    }

    /** The lesson and the ones after it in its series; the reason goes to the students (Fase 22). */
    @PostMapping("/{id}/cancel-following")
    LessonsResponse cancelFollowing(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                                    @Valid @RequestBody(required = false) CancelLessonRequest request) {
        requireTeacher(caller);
        String reason = request == null ? null : request.reason();
        return new LessonsResponse(cancelLesson.cancelFollowing(caller.id(), id, reason).stream()
                .map(lesson -> LessonResponse.from(lesson, true))
                .toList());
    }

    @PatchMapping("/{id}")
    LessonResponse edit(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                        @Valid @RequestBody EditLessonRequest request) {
        requireTeacher(caller);
        return LessonResponse.from(editLesson.edit(caller.id(), id,
                new EditLesson.Changes(request.notes(), request.capacity())), true);
    }

    /** The body is optional: it only carries the reason, which goes to the students. */
    @PostMapping("/{id}/cancel")
    LessonResponse cancel(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                          @Valid @RequestBody(required = false) CancelLessonRequest request) {
        requireTeacher(caller);
        String reason = request == null ? null : request.reason();
        return LessonResponse.from(cancelLesson.cancel(caller.id(), id, reason), true);
    }

    @PostMapping("/cancellations")
    CancelledDayResponse cancelDay(@AuthenticationPrincipal AuthenticatedUser caller,
                                   @Valid @RequestBody CancelDayRequest request) {
        requireTeacher(caller);
        return new CancelledDayResponse(cancelLesson.cancelDay(caller.id(), request.date(), request.reason()).stream()
                .map(lesson -> LessonResponse.from(lesson, true))
                .toList());
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher can manage lessons");
        }
    }
}

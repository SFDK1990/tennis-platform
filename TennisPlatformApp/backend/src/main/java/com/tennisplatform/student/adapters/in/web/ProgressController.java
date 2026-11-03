package com.tennisplatform.student.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.student.application.port.in.StudentProgress;
import com.tennisplatform.student.application.port.in.StudentProgressView;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The objectives of each level that the teacher marks as achieved (44-fase29-analisis-progreso.md):
 * the teacher reads and marks them in the record of a student they manage, and the student reads
 * their own.
 */
@RestController
class ProgressController {

    private final StudentProgress progress;

    ProgressController(StudentProgress progress) {
        this.progress = progress;
    }

    @GetMapping("/api/v1/teacher/students/{userId}/progress")
    StudentProgressView ofStudent(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID userId) {
        requireTeacher(caller);
        return progress.ofManagedStudent(caller.id(), userId);
    }

    /** The body may be left out: the day is then today, in the teacher's zone. */
    @PutMapping("/api/v1/teacher/students/{userId}/objectives/{objectiveId}")
    StudentProgressView mark(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID userId,
                             @PathVariable UUID objectiveId, @RequestBody(required = false) MarkRequest request) {
        requireTeacher(caller);
        return progress.mark(caller.id(), userId, objectiveId, request == null ? null : request.achievedOn());
    }

    @DeleteMapping("/api/v1/teacher/students/{userId}/objectives/{objectiveId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unmark(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID userId,
                @PathVariable UUID objectiveId) {
        requireTeacher(caller);
        progress.unmark(caller.id(), userId, objectiveId);
    }

    @GetMapping("/api/v1/me/progress")
    StudentProgressView mine(@AuthenticationPrincipal AuthenticatedUser caller) {
        if (!caller.isStudent()) {
            throw ForbiddenOperationException.roleNotAllowed("Only a student has progress");
        }
        return progress.ofStudent(caller.id());
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (!caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher marks objectives");
        }
    }

    record MarkRequest(LocalDate achievedOn) {
    }
}

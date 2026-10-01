package com.tennisplatform.lesson.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.LessonResponse;
import com.tennisplatform.lesson.application.port.in.CancelLesson;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The administrator resolving an incident (30-fase19-analisis-cierre-mvp.md). Its own route
 * rather than letting the admin through {@code /teacher/lessons/{id}/cancel}: the prefix says
 * who may call it, and the bookings it cancels record a different decider.
 */
@RestController
class AdminLessonsController {

    private final CancelLesson cancelLesson;

    AdminLessonsController(CancelLesson cancelLesson) {
        this.cancelLesson = cancelLesson;
    }

    @PostMapping("/api/v1/admin/lessons/{id}/cancel")
    LessonResponse cancel(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        if (!caller.isAdmin()) {
            throw ForbiddenOperationException.roleNotAllowed("Only an administrator resolves incidents");
        }
        return LessonResponse.from(cancelLesson.cancelAsAdmin(id), false);
    }
}

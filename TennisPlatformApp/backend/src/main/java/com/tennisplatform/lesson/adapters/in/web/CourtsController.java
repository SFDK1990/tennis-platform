package com.tennisplatform.lesson.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.lesson.adapters.in.web.LessonDtos.RenameCourtRequest;
import com.tennisplatform.lesson.application.port.in.Courts;
import com.tennisplatform.lesson.application.port.in.Courts.CourtView;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The school's courts (Fase 25.6): anybody signed in reads them, only the teacher renames them. */
@RestController
class CourtsController {

    private final Courts courts;

    CourtsController(Courts courts) {
        this.courts = courts;
    }

    @GetMapping("/api/v1/courts")
    CourtsResponse list() {
        return new CourtsResponse(courts.all());
    }

    @PatchMapping("/api/v1/teacher/courts/{id}")
    CourtView rename(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                     @Valid @RequestBody RenameCourtRequest request) {
        if (!caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher renames the courts");
        }
        return courts.rename(caller.id(), id, request.name());
    }

    record CourtsResponse(List<CourtView> courts) {
    }
}

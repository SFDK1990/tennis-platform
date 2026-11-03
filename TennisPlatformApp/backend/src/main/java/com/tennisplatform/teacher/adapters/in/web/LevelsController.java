package com.tennisplatform.teacher.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.teacher.application.port.in.LevelView;
import com.tennisplatform.teacher.application.port.in.ManageLevels;
import com.tennisplatform.teacher.application.port.in.ObjectiveView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The teacher's levels (Fase 23) and their objectives (Fase 29): anybody signed in reads them, only
 * the teacher changes them.
 */
@RestController
class LevelsController {

    private final ManageLevels levels;

    LevelsController(ManageLevels levels) {
        this.levels = levels;
    }

    @GetMapping("/api/v1/levels")
    LevelsResponse list() {
        return new LevelsResponse(levels.ofTheTeacher().stream().map(LevelResponse::from).toList());
    }

    @PostMapping("/api/v1/teacher/levels")
    @ResponseStatus(HttpStatus.CREATED)
    LevelResponse create(@AuthenticationPrincipal AuthenticatedUser caller,
                         @Valid @RequestBody CreateLevelRequest request) {
        requireTeacher(caller);
        return LevelResponse.from(levels.create(caller.id(), request.name()));
    }

    @PatchMapping("/api/v1/teacher/levels/{id}")
    LevelResponse update(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                         @Valid @RequestBody UpdateLevelRequest request) {
        requireTeacher(caller);
        return LevelResponse.from(levels.update(caller.id(), id, request.name(), request.position()));
    }

    @DeleteMapping("/api/v1/teacher/levels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        levels.delete(caller.id(), id);
    }

    @PostMapping("/api/v1/teacher/levels/{id}/objectives")
    @ResponseStatus(HttpStatus.CREATED)
    ObjectiveResponse addObjective(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                                   @Valid @RequestBody CreateObjectiveRequest request) {
        requireTeacher(caller);
        return ObjectiveResponse.from(levels.addObjective(caller.id(), id, request.title()));
    }

    @PatchMapping("/api/v1/teacher/objectives/{id}")
    ObjectiveResponse updateObjective(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                                      @Valid @RequestBody UpdateObjectiveRequest request) {
        requireTeacher(caller);
        return ObjectiveResponse.from(levels.updateObjective(caller.id(), id, request.title(), request.position()));
    }

    /** Retires it: whoever achieved it keeps it (Fase 29). */
    @DeleteMapping("/api/v1/teacher/objectives/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void retireObjective(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        levels.retireObjective(caller.id(), id);
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher manages the levels");
        }
    }

    record LevelsResponse(List<LevelResponse> levels) {
    }

    /** A level with its live objectives in their order. */
    record LevelResponse(UUID id, String name, int position, List<ObjectiveResponse> objectives) {

        static LevelResponse from(LevelView view) {
            return new LevelResponse(view.id(), view.name(), view.position(),
                    view.objectives().stream().map(ObjectiveResponse::from).toList());
        }
    }

    record ObjectiveResponse(UUID id, String title, int position) {

        static ObjectiveResponse from(ObjectiveView view) {
            return new ObjectiveResponse(view.id(), view.title(), view.position());
        }
    }

    record CreateObjectiveRequest(@NotNull String title) {
    }

    /** Either field may be left out. */
    record UpdateObjectiveRequest(String title, @Positive Integer position) {
    }

    record CreateLevelRequest(@NotNull String name) {
    }

    /** Either field may be left out. */
    record UpdateLevelRequest(String name, @Positive Integer position) {
    }
}

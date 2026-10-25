package com.tennisplatform.teacher.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.teacher.application.port.in.LevelView;
import com.tennisplatform.teacher.application.port.in.ManageLevels;
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

/** The teacher's levels (Fase 23): anybody signed in reads them, only the teacher changes them. */
@RestController
class LevelsController {

    private final ManageLevels levels;

    LevelsController(ManageLevels levels) {
        this.levels = levels;
    }

    @GetMapping("/api/v1/levels")
    LevelsResponse list() {
        return new LevelsResponse(levels.ofTheTeacher());
    }

    @PostMapping("/api/v1/teacher/levels")
    @ResponseStatus(HttpStatus.CREATED)
    LevelView create(@AuthenticationPrincipal AuthenticatedUser caller, @Valid @RequestBody CreateLevelRequest request) {
        requireTeacher(caller);
        return levels.create(caller.id(), request.name());
    }

    @PatchMapping("/api/v1/teacher/levels/{id}")
    LevelView update(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                     @Valid @RequestBody UpdateLevelRequest request) {
        requireTeacher(caller);
        return levels.update(caller.id(), id, request.name(), request.position());
    }

    @DeleteMapping("/api/v1/teacher/levels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        levels.delete(caller.id(), id);
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher manages the levels");
        }
    }

    record LevelsResponse(List<LevelView> levels) {
    }

    record CreateLevelRequest(@NotNull String name) {
    }

    /** Either field may be left out. */
    record UpdateLevelRequest(String name, @Positive Integer position) {
    }
}

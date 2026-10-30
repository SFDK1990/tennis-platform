package com.tennisplatform.pass.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.pass.application.port.in.ManagePassTypes;
import com.tennisplatform.pass.application.port.in.ManagePassTypes.NewPassType;
import com.tennisplatform.pass.application.port.in.ManagePassTypes.PassTypeChange;
import com.tennisplatform.pass.application.port.in.PassTypeView;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The teacher's pass types and the switch that makes them required (Fase 27). Anybody signed in
 * reads them, so a student knows what there is and whether they need one; only the teacher
 * changes them.
 */
@RestController
@RequestMapping("/api/v1")
class PassTypesController {

    private final ManagePassTypes passTypes;

    PassTypesController(ManagePassTypes passTypes) {
        this.passTypes = passTypes;
    }

    /** A student is not shown the retired types: they are no longer sold. */
    @GetMapping("/pass-types")
    PassTypesResponse list(@AuthenticationPrincipal AuthenticatedUser caller) {
        List<PassTypeView> types = passTypes.ofTheTeacher().stream()
                .filter(type -> caller.isTeacher() || !type.retired())
                .toList();
        return new PassTypesResponse(passTypes.passesRequired(), types);
    }

    @PostMapping("/teacher/pass-types")
    @ResponseStatus(HttpStatus.CREATED)
    PassTypeView create(@AuthenticationPrincipal AuthenticatedUser caller,
                        @Valid @RequestBody CreatePassTypeRequest request) {
        requireTeacher(caller);
        return passTypes.create(caller.id(), new NewPassType(request.name(), request.lessonType(), request.lessons(),
                request.priceCents(), request.validityMonths()));
    }

    @PatchMapping("/teacher/pass-types/{id}")
    PassTypeView update(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                        @Valid @RequestBody UpdatePassTypeRequest request) {
        requireTeacher(caller);
        return passTypes.update(caller.id(), id, new PassTypeChange(request.name(), request.lessonType(),
                request.lessons(), request.priceCents(), request.validityMonths(),
                Boolean.TRUE.equals(request.neverExpires()), request.retired()));
    }

    @DeleteMapping("/teacher/pass-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        passTypes.delete(caller.id(), id);
    }

    @PutMapping("/teacher/pass-settings")
    PassSettingsResponse settings(@AuthenticationPrincipal AuthenticatedUser caller,
                                  @Valid @RequestBody PassSettingsRequest request) {
        requireTeacher(caller);
        return new PassSettingsResponse(passTypes.requirePasses(caller.id(), request.required()));
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher manages the passes");
        }
    }

    record PassTypesResponse(boolean required, List<PassTypeView> passTypes) {
    }

    record CreatePassTypeRequest(@NotNull String name, @NotNull String lessonType, @NotNull Integer lessons,
                                 @NotNull Integer priceCents, Integer validityMonths) {
    }

    /** Every field optional. {@code neverExpires} true removes the validity. */
    record UpdatePassTypeRequest(String name, String lessonType, Integer lessons, Integer priceCents,
                                 Integer validityMonths, Boolean neverExpires, Boolean retired) {
    }

    record PassSettingsRequest(@NotNull Boolean required) {
    }

    record PassSettingsResponse(boolean required) {
    }
}

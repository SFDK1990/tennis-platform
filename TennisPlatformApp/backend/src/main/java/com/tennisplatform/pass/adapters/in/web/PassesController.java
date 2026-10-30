package com.tennisplatform.pass.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.pass.application.port.in.ManagePassTypes;
import com.tennisplatform.pass.application.port.in.ManagePasses;
import com.tennisplatform.pass.application.port.in.PassView;
import com.tennisplatform.pass.application.port.in.QueryPasses;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import java.util.List;
import java.util.UUID;

/**
 * The passes of the students (Fase 27): the teacher records and voids them, the student reads
 * their own. Paying happens outside the app; this only writes down that it did.
 */
@RestController
@RequestMapping("/api/v1")
class PassesController {

    private final ManagePasses passes;
    private final QueryPasses query;
    private final ManagePassTypes passTypes;

    PassesController(ManagePasses passes, QueryPasses query, ManagePassTypes passTypes) {
        this.passes = passes;
        this.query = query;
        this.passTypes = passTypes;
    }

    /** All the passes of the teacher's students, or of one: the list of students flags who is short. */
    @GetMapping("/teacher/passes")
    PassesResponse ofTeacher(@AuthenticationPrincipal AuthenticatedUser caller,
                             @RequestParam(required = false) UUID studentUserId) {
        requireTeacher(caller);
        return new PassesResponse(passTypes.passesRequired(), passes.ofTeacher(caller.id(), studentUserId));
    }

    @PostMapping("/teacher/students/{userId}/passes")
    @ResponseStatus(HttpStatus.CREATED)
    PassView record(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID userId,
                    @Valid @RequestBody RecordPassRequest request) {
        requireTeacher(caller);
        return passes.record(caller.id(), userId, request.passTypeId(), request.paidOn(), request.paymentMethod());
    }

    @PostMapping("/teacher/passes/{id}/void")
    PassView voidPass(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        return passes.voidPass(caller.id(), id);
    }

    @GetMapping("/me/passes")
    PassesResponse mine(@AuthenticationPrincipal AuthenticatedUser caller) {
        if (!caller.isStudent()) {
            throw ForbiddenOperationException.roleNotAllowed("Only a student has passes");
        }
        return new PassesResponse(passTypes.passesRequired(), query.ofStudent(caller.id()));
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher manages the passes");
        }
    }

    /** {@code required}: whether the teacher has switched on "no booking without a pass". */
    record PassesResponse(boolean required, List<PassView> items) {
    }

    record RecordPassRequest(@NotNull UUID passTypeId, @NotNull LocalDate paidOn, @NotNull String paymentMethod) {
    }
}

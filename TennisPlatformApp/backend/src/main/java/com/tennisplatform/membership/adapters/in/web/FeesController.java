package com.tennisplatform.membership.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.membership.application.port.in.FeeLineView;
import com.tennisplatform.membership.application.port.in.FeeView;
import com.tennisplatform.membership.application.port.in.ManageFees;
import com.tennisplatform.membership.application.port.in.ManageFees.NewFee;
import com.tennisplatform.membership.domain.InvalidMembershipException;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/** The monthly fees (Fase 28): recorded by the teacher, never charged in the app. */
@RestController
@RequestMapping("/api/v1/teacher")
class FeesController {

    private final ManageFees fees;

    FeesController(ManageFees fees) {
        this.fees = fees;
    }

    @GetMapping("/fees")
    FeesResponse lines(@AuthenticationPrincipal AuthenticatedUser caller,
                       @RequestParam(required = false) String month,
                       @RequestParam(required = false) UUID studentUserId) {
        requireTeacher(caller);
        return new FeesResponse(fees.dueDay(caller.id()),
                fees.lines(caller.id(), month == null ? null : parse(month), studentUserId));
    }

    @PostMapping("/fees")
    @ResponseStatus(HttpStatus.CREATED)
    FeeView record(@AuthenticationPrincipal AuthenticatedUser caller, @Valid @RequestBody RecordFeeRequest request) {
        requireTeacher(caller);
        return fees.record(caller.id(), new NewFee(request.membershipId(), parse(request.month()), request.kind(),
                request.amountCents(), request.paidOn(), request.paymentMethod()));
    }

    @PostMapping("/fees/{id}/void")
    FeeView voidFee(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        return fees.voidFee(caller.id(), id);
    }

    @PutMapping("/fee-settings")
    FeeSettingsResponse changeSettings(@AuthenticationPrincipal AuthenticatedUser caller,
                                       @Valid @RequestBody FeeSettingsRequest request) {
        requireTeacher(caller);
        return new FeeSettingsResponse(fees.changeDueDay(caller.id(), request.dueDay()));
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher manages the fees");
        }
    }

    /** Parsed here: Spring's converter would answer a malformed month with a 500. */
    private static YearMonth parse(String month) {
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidMembershipException("A month is written as 2026-11, not " + month);
        }
    }

    /** {@code dueDay}: the day of the month by which a fee is due (decision 3). */
    record FeesResponse(int dueDay, List<FeeLineView> items) {
    }

    record RecordFeeRequest(@NotNull UUID membershipId, @NotNull String month, @NotNull String kind,
                            Integer amountCents, LocalDate paidOn, String paymentMethod) {
    }

    record FeeSettingsRequest(@NotNull Integer dueDay) {
    }

    record FeeSettingsResponse(int dueDay) {
    }
}

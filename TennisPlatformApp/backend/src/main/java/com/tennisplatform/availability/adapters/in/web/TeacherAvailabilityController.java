package com.tennisplatform.availability.adapters.in.web;

import com.tennisplatform.availability.adapters.in.web.AvailabilityDtos.AvailabilityResponse;
import com.tennisplatform.availability.adapters.in.web.AvailabilityDtos.CreateAvailabilityExceptionRequest;
import com.tennisplatform.availability.adapters.in.web.AvailabilityDtos.AvailabilityExceptionResponse;
import com.tennisplatform.availability.adapters.in.web.AvailabilityDtos.WeeklyAvailabilityRequest;
import com.tennisplatform.availability.adapters.in.web.AvailabilityDtos.WeeklyAvailabilityResponse;
import com.tennisplatform.availability.application.port.in.ConfigureWeeklyAvailability;
import com.tennisplatform.availability.application.port.in.GetAvailability;
import com.tennisplatform.availability.application.port.in.ManageAvailabilityExceptions;
import com.tennisplatform.availability.application.port.in.NewAvailabilityOverride;
import com.tennisplatform.availability.application.port.in.WeeklyRuleCommand;
import com.tennisplatform.availability.domain.TeacherRoleRequiredException;
import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.util.UUID;

/**
 * The teacher's schedule.
 *
 * <p>Read by anyone authenticated, written only by the teacher - the same split the teacher
 * profile already uses. A student has to know when their teacher works, and working hours are
 * what the platform exists to publish; nothing personal travels through here.
 *
 * <p>The role is checked here and the ownership in the service. Neither replaces the other: the
 * role says what kind of account is calling, ownership says it is the account that owns the
 * schedule being changed.
 */
@RestController
@RequestMapping("/api/v1/teacher/availability")
class TeacherAvailabilityController {

    private final GetAvailability getAvailability;
    private final ConfigureWeeklyAvailability configureWeekly;
    private final ManageAvailabilityExceptions manageExceptions;

    TeacherAvailabilityController(GetAvailability getAvailability,
                                  ConfigureWeeklyAvailability configureWeekly,
                                  ManageAvailabilityExceptions manageExceptions) {
        this.getAvailability = getAvailability;
        this.configureWeekly = configureWeekly;
        this.manageExceptions = manageExceptions;
    }

    /**
     * The range is mandatory rather than defaulted. A default would make the answer depend on
     * today's date without the caller knowing, and exceptions accumulate for ever - an
     * unbounded read here is one that gets slower every season until somebody notices.
     */
    @GetMapping
    AvailabilityResponse get(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return AvailabilityResponse.from(getAvailability.get(from, to));
    }

    @PutMapping("/weekly")
    WeeklyAvailabilityResponse replaceWeekly(@AuthenticationPrincipal AuthenticatedUser caller,
                                             @Valid @RequestBody WeeklyAvailabilityRequest request) {
        requireTeacher(caller);
        return WeeklyAvailabilityResponse.of(configureWeekly.replace(caller.id(),
                request.rules().stream()
                        .map(rule -> new WeeklyRuleCommand(rule.dayOfWeek(), rule.startTime(),
                                rule.endTime(), rule.activeFrom(), rule.activeUntil()))
                        .toList()));
    }

    @PostMapping("/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    AvailabilityExceptionResponse addException(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @Valid @RequestBody CreateAvailabilityExceptionRequest request) {
        requireTeacher(caller);
        return AvailabilityExceptionResponse.from(manageExceptions.add(caller.id(),
                new NewAvailabilityOverride(request.date(), request.startTime(), request.endTime(),
                        request.type())));
    }

    @DeleteMapping("/exceptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeException(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        manageExceptions.remove(caller.id(), id);
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw new TeacherRoleRequiredException("Only the teacher can change the availability");
        }
    }
}

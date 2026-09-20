package com.tennisplatform.availability.adapters.in.web;

import com.tennisplatform.availability.application.port.in.AvailabilityView;
import com.tennisplatform.availability.application.port.in.AvailabilityOverrideView;
import com.tennisplatform.availability.application.port.in.WeeklyRuleView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Wire representations. Kept apart from the domain so persistence can never leak out. */
final class AvailabilityDtos {

    private AvailabilityDtos() {
    }

    record WeeklyRuleResponse(UUID id, String dayOfWeek, LocalTime startTime, LocalTime endTime,
                              LocalDate activeFrom, LocalDate activeUntil) {

        static WeeklyRuleResponse from(WeeklyRuleView view) {
            return new WeeklyRuleResponse(view.id(), view.dayOfWeek(), view.startTime(),
                    view.endTime(), view.activeFrom(), view.activeUntil());
        }
    }

    /** Named after the {@code AvailabilityException} schema of openapi.yaml, which is the wire contract. */
    record AvailabilityExceptionResponse(UUID id, LocalDate date, LocalTime startTime,
                                         LocalTime endTime, String type) {

        static AvailabilityExceptionResponse from(AvailabilityOverrideView view) {
            return new AvailabilityExceptionResponse(view.id(), view.date(), view.startTime(),
                    view.endTime(), view.type());
        }
    }

    record AvailabilityResponse(List<WeeklyRuleResponse> weeklyRules,
                                List<AvailabilityExceptionResponse> exceptions) {

        static AvailabilityResponse from(AvailabilityView view) {
            return new AvailabilityResponse(
                    view.weeklyRules().stream().map(WeeklyRuleResponse::from).toList(),
                    view.exceptions().stream().map(AvailabilityExceptionResponse::from).toList());
        }
    }

    record WeeklyAvailabilityResponse(List<WeeklyRuleResponse> rules) {

        static WeeklyAvailabilityResponse of(List<WeeklyRuleView> views) {
            return new WeeklyAvailabilityResponse(
                    views.stream().map(WeeklyRuleResponse::from).toList());
        }
    }

    /**
     * The whole weekly configuration, because the endpoint replaces it rather than patching it.
     *
     * <p>An empty list is a legitimate body: it means "I have no fixed weekly schedule". That is
     * why {@code rules} is only {@code @NotNull} and not {@code @NotEmpty} - refusing an empty
     * set would leave the teacher no way to clear their week.
     */
    record WeeklyAvailabilityRequest(@NotNull @Valid List<WeeklyRuleRequest> rules) {
    }

    /**
     * The day is a name - MONDAY to SUNDAY - and not a number, which settles the collision
     * between the three numbering conventions that meet at this field. The times are validated
     * in the domain, where the "end after start" rule holds for every caller and not only for
     * this one.
     */
    record WeeklyRuleRequest(@NotNull String dayOfWeek,
                             @NotNull LocalTime startTime,
                             @NotNull LocalTime endTime,
                             LocalDate activeFrom,
                             LocalDate activeUntil) {
    }

    /** Times are optional here: a BLOCK without them is a whole day. The domain rejects the rest. */
    record CreateAvailabilityExceptionRequest(@NotNull LocalDate date,
                                      LocalTime startTime,
                                      LocalTime endTime,
                                      @NotNull String type) {
    }
}

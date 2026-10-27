package com.tennisplatform.calendar.adapters.in.web;

import com.tennisplatform.calendar.application.port.in.GetMonthSummary;
import com.tennisplatform.calendar.application.port.in.GetMonthSummary.MonthSummaryView;
import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.shared.domain.DateRange.InvalidDateRangeException;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** The teacher's monthly summary (Fase 25). Only the teacher sees it. */
@RestController
class MonthSummaryController {

    private final GetMonthSummary summaries;

    MonthSummaryController(GetMonthSummary summaries) {
        this.summaries = summaries;
    }

    @GetMapping("/api/v1/teacher/summary")
    MonthSummaryView summary(@AuthenticationPrincipal AuthenticatedUser caller,
                             @RequestParam(required = false) String month) {
        if (!caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher sees the monthly summary");
        }
        return summaries.forTeacher(caller.id(), month == null ? null : parse(month));
    }

    /** Parsed here: Spring's converter would answer a malformed month with a 500. */
    private static YearMonth parse(String month) {
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidDateRangeException("A month is written as 2026-10, not " + month);
        }
    }
}

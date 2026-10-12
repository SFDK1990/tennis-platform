package com.tennisplatform.calendar.adapters.in.web;

import com.tennisplatform.calendar.application.port.in.CalendarView;
import com.tennisplatform.calendar.application.port.in.GetCalendar;
import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** The view records are already the wire shape; there is nothing to hide from either side. */
@RestController
class CalendarController {

    private final GetCalendar getCalendar;

    CalendarController(GetCalendar getCalendar) {
        this.getCalendar = getCalendar;
    }

    @GetMapping("/api/v1/calendar")
    CalendarView calendar(@AuthenticationPrincipal AuthenticatedUser caller,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (caller.isTeacher()) {
            return getCalendar.forTeacher(caller.id(), from, to);
        }
        if (caller.isStudent()) {
            return getCalendar.forStudent(caller.id(), from, to);
        }
        if (caller.isAdmin()) {
            return getCalendar.forAdministration(from, to);
        }
        throw ForbiddenOperationException.roleNotAllowed("Only the teacher, students and the administrator have a calendar");
    }
}

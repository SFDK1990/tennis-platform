package com.tennisplatform.booking.adapters.in.web;

import com.tennisplatform.booking.adapters.in.web.BookingDtos.BookingResponse;
import com.tennisplatform.booking.adapters.in.web.BookingDtos.MarkAttendanceRequest;
import com.tennisplatform.booking.application.port.in.MarkAttendance;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Attendance, served from {@code booking} although the path hangs under {@code /teacher/lessons}:
 * the body is booking ids and the answer is bookings, so this is the module that can serve it.
 * A path does not say which module answers it. Fase 8 left it here on purpose.
 */
@RestController
class AttendanceController {

    private final MarkAttendance markAttendance;

    AttendanceController(MarkAttendance markAttendance) {
        this.markAttendance = markAttendance;
    }

    @PostMapping("/api/v1/teacher/lessons/{id}/attendance")
    List<BookingResponse> mark(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                               @Valid @RequestBody MarkAttendanceRequest request) {
        if (!caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher can record attendance");
        }
        List<MarkAttendance.Entry> entries = request.entries().stream()
                .map(entry -> new MarkAttendance.Entry(entry.bookingId(), entry.status()))
                .toList();
        return markAttendance.mark(caller.id(), id, entries).stream().map(BookingResponse::from).toList();
    }
}

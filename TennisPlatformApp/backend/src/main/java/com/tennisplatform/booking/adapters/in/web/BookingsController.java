package com.tennisplatform.booking.adapters.in.web;

import com.tennisplatform.booking.adapters.in.web.BookingDtos.BookingPageResponse;
import com.tennisplatform.booking.adapters.in.web.BookingDtos.BookingResponse;
import com.tennisplatform.booking.application.port.in.BookLesson;
import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.in.CancelBooking;
import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Booking, listing and cancelling. The role decides which use case answers; the use case decides
 * which rows the caller may touch. Neither replaces the other.
 */
@RestController
@RequestMapping("/api/v1")
class BookingsController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookLesson bookLesson;
    private final GetBookings getBookings;
    private final CancelBooking cancelBooking;

    BookingsController(BookLesson bookLesson, GetBookings getBookings, CancelBooking cancelBooking) {
        this.bookLesson = bookLesson;
        this.getBookings = getBookings;
        this.cancelBooking = cancelBooking;
    }

    @PostMapping("/lessons/{id}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponse book(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        if (!caller.isStudent()) {
            throw ForbiddenOperationException.roleNotAllowed("Only a student can book a seat in a lesson");
        }
        return BookingResponse.from(bookLesson.book(caller.id(), caller.emailVerified(), id));
    }

    @GetMapping("/bookings")
    BookingPageResponse list(@AuthenticationPrincipal AuthenticatedUser caller,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) UUID lessonId,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        if (caller.isStudent()) {
            return BookingPageResponse.from(getBookings.forStudent(caller.id(), status, safePage, safeSize));
        }
        if (caller.isTeacher()) {
            return BookingPageResponse.from(
                    getBookings.forTeacher(caller.id(), lessonId, status, safePage, safeSize));
        }
        if (caller.isAdmin()) {
            return BookingPageResponse.from(getBookings.forAdministration(lessonId, status, safePage, safeSize));
        }
        throw ForbiddenOperationException.roleNotAllowed("Bookings are listed for a student, the teacher or the administrator");
    }

    @PostMapping("/bookings/{id}/cancel")
    BookingResponse cancel(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        BookingView cancelled;
        if (caller.isStudent()) {
            cancelled = cancelBooking.asStudent(caller.id(), id);
        } else if (caller.isTeacher()) {
            cancelled = cancelBooking.asTeacher(caller.id(), id);
        } else if (caller.isAdmin()) {
            cancelled = cancelBooking.asAdmin(id);
        } else {
            throw ForbiddenOperationException.roleNotAllowed("This account cannot cancel bookings");
        }
        return BookingResponse.from(cancelled);
    }
}

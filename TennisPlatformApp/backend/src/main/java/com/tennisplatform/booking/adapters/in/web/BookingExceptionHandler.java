package com.tennisplatform.booking.adapters.in.web;

import com.tennisplatform.booking.domain.AttendanceNotYetOpenException;
import com.tennisplatform.booking.domain.BookingAlreadyCancelledException;
import com.tennisplatform.booking.domain.BookingAlreadyExistsException;
import com.tennisplatform.booking.domain.BookingNotFoundException;
import com.tennisplatform.booking.domain.CancellationWindowExpiredException;
import com.tennisplatform.booking.domain.EmailNotVerifiedException;
import com.tennisplatform.booking.domain.InvalidBookingRequestException;
import com.tennisplatform.booking.domain.LessonAlreadyStartedException;
import com.tennisplatform.booking.domain.LessonFullException;
import com.tennisplatform.booking.domain.LessonNotBookableException;
import com.tennisplatform.booking.domain.StudentScheduleOverlapException;
import com.tennisplatform.error.Problems;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps booking failures to Problem Details, with the 409/422 split of 11-contrato-api.md: 409 when
 * re-reading may change the answer - a seat may free up, the cancelled lesson was on a stale
 * screen - and 422 when it will not, because the clock only moves one way.
 *
 * <p>A lesson that does not exist is not mapped here: {@code LockLesson} raises {@code lesson}'s
 * own exception, and {@code LessonExceptionHandler} already selects by exception type, whoever
 * raised it.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class BookingExceptionHandler {

    @ExceptionHandler(LessonFullException.class)
    ProblemDetail handleFull(LessonFullException e) {
        return Problems.of(HttpStatus.CONFLICT, "Lesson full", e.getMessage(), "LESSON_FULL");
    }

    @ExceptionHandler(BookingAlreadyExistsException.class)
    ProblemDetail handleAlreadyExists(BookingAlreadyExistsException e) {
        return Problems.of(HttpStatus.CONFLICT, "Already booked", e.getMessage(), "BOOKING_ALREADY_EXISTS");
    }

    @ExceptionHandler(StudentScheduleOverlapException.class)
    ProblemDetail handleOverlap(StudentScheduleOverlapException e) {
        return Problems.of(HttpStatus.CONFLICT, "Overlapping booking", e.getMessage(),
                "STUDENT_SCHEDULE_OVERLAP");
    }

    @ExceptionHandler(LessonNotBookableException.class)
    ProblemDetail handleNotBookable(LessonNotBookableException e) {
        return Problems.of(HttpStatus.CONFLICT, "Lesson not bookable", e.getMessage(), "LESSON_NOT_BOOKABLE");
    }

    @ExceptionHandler(BookingAlreadyCancelledException.class)
    ProblemDetail handleAlreadyCancelled(BookingAlreadyCancelledException e) {
        return Problems.of(HttpStatus.CONFLICT, "Booking already cancelled", e.getMessage(),
                "BOOKING_ALREADY_CANCELLED");
    }

    @ExceptionHandler(LessonAlreadyStartedException.class)
    ProblemDetail handleAlreadyStarted(LessonAlreadyStartedException e) {
        return Problems.of(HttpStatus.UNPROCESSABLE_ENTITY, "Lesson already started", e.getMessage(),
                "LESSON_ALREADY_STARTED");
    }

    @ExceptionHandler(CancellationWindowExpiredException.class)
    ProblemDetail handleWindowExpired(CancellationWindowExpiredException e) {
        return Problems.of(HttpStatus.UNPROCESSABLE_ENTITY, "Too late to cancel", e.getMessage(),
                "CANCELLATION_WINDOW_EXPIRED");
    }

    @ExceptionHandler(AttendanceNotYetOpenException.class)
    ProblemDetail handleAttendanceTooEarly(AttendanceNotYetOpenException e) {
        return Problems.of(HttpStatus.UNPROCESSABLE_ENTITY, "Too early for attendance", e.getMessage(),
                "ATTENDANCE_NOT_YET_OPEN");
    }

    @ExceptionHandler(BookingNotFoundException.class)
    ProblemDetail handleNotFound(BookingNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Booking not found", e.getMessage(), "BOOKING_NOT_FOUND");
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    ProblemDetail handleNotVerified(EmailNotVerifiedException e) {
        return Problems.of(HttpStatus.FORBIDDEN, "Email not verified", e.getMessage(), "EMAIL_NOT_VERIFIED");
    }

    @ExceptionHandler(InvalidBookingRequestException.class)
    ProblemDetail handleInvalid(InvalidBookingRequestException e) {
        return Problems.of(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage(), "VALIDATION_ERROR");
    }
}

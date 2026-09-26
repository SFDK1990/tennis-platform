package com.tennisplatform.error;

import com.tennisplatform.shared.domain.DateRange;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("code", "VALIDATION_ERROR");
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldError)
            .toList();
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }

    /**
     * A path or query value of the wrong type, a required parameter missing, or a body that is
     * not valid JSON. Without these three, Spring's own exceptions fell through to the catch-all
     * below and a malformed id answered 500 instead of 400.
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public ProblemDetail handleMalformedRequest(Exception ex) {
        return Problems.of(HttpStatus.BAD_REQUEST, "Malformed request",
                "The request has a missing or malformed value", "VALIDATION_ERROR");
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbidden(ForbiddenOperationException ex) {
        return Problems.of(HttpStatus.FORBIDDEN, "Not allowed", ex.getMessage(), ex.code());
    }

    @ExceptionHandler(DateRange.InvalidDateRangeException.class)
    public ProblemDetail handleDateRange(DateRange.InvalidDateRangeException ex) {
        return Problems.of(HttpStatus.BAD_REQUEST, "Invalid date range", ex.getMessage(),
                "DATE_RANGE_INVALID");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        String detail = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), detail);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private Map<String, String> toFieldError(FieldError fieldError) {
        return Map.of(
            "field", fieldError.getField(),
            "message", fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage()
        );
    }
}

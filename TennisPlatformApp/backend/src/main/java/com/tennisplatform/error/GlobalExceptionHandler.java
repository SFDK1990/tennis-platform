package com.tennisplatform.error;

import com.tennisplatform.shared.domain.DateRange;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /*
     * Protocol errors: the client asked for something the API does not have, in a way it does
     * not speak. Without these handlers they fell through to the catch-all below and answered
     * 500 with an ERROR trace - so any scanner filled the log with false alarms and buried the
     * real ones (26-fase15-analisis-seguridad.md). They are the client's mistake: DEBUG, not
     * ERROR.
     */

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoRoute(NoResourceFoundException ex) {
        log.debug("No route: {}", ex.getMessage());
        return Problems.of(HttpStatus.NOT_FOUND, "Not found",
                "There is nothing at this address", "NOT_FOUND");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.debug("Method not allowed: {}", ex.getMessage());
        HttpHeaders headers = new HttpHeaders();
        var supported = ex.getSupportedHttpMethods();
        if (supported != null) {
            headers.setAllow(supported);
        }
        return new ResponseEntity<>(Problems.of(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed",
                "This address does not accept " + ex.getMethod(), "METHOD_NOT_ALLOWED"),
                headers, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        log.debug("Unsupported media type: {}", ex.getMessage());
        return Problems.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type",
                "The body must be application/json", "UNSUPPORTED_MEDIA_TYPE");
    }

    /**
     * No body: the client said it cannot read JSON, and problem+json is JSON. Writing one anyway
     * would fail the same negotiation a second time.
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        log.debug("Not acceptable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
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
        log.error("Unhandled exception", new WithoutMessage(ex, 0));
        // Nothing from the exception reaches the client: its message can name tables, columns
        // or values. The correlation id in the response header is what links it to this log line.
        return Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "Unexpected error", "INTERNAL_ERROR");
    }

    /**
     * The trace of an unexpected failure, with the type of each exception and every frame but
     * none of their messages. A message can quote what the database was given - "(email)=(...)"
     * - and the log keeps no personal data (28-fase16-analisis-observabilidad.md). The type and
     * the frames say what failed and where; the correlation id says which request it was.
     */
    static final class WithoutMessage extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /** A cause chain that loops back on itself must not recurse forever. */
        private static final int MAX_CAUSES = 20;

        WithoutMessage(Throwable original, int depth) {
            super(original.getClass().getName(),
                    original.getCause() == null || depth >= MAX_CAUSES
                            ? null
                            : new WithoutMessage(original.getCause(), depth + 1),
                    false, true);
            setStackTrace(original.getStackTrace());
        }
    }

    private Map<String, String> toFieldError(FieldError fieldError) {
        return Map.of(
            "field", fieldError.getField(),
            "message", fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage()
        );
    }
}

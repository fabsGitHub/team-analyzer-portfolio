package com.teamanalyzer.teamanalyzer.web;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.net.URI;
import java.util.*;

import com.teamanalyzer.teamanalyzer.port.AppClock;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

    @Value("${app.errors.include-stacktrace:false}")
    private boolean includeStacktrace;

    private final AppClock clock;

    // ---- Helpers -------------------------------------------------------------

    private URI currentRequestUri(HttpServletRequest req) {
        try {
            return ServletUriComponentsBuilder.fromRequest(req).build().toUri();
        } catch (Exception e) {
            return URI.create(req.getRequestURI());
        }
    }

    private ProblemDetail pd(HttpStatus status, String title, String detail, HttpServletRequest req) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status,
                Optional.ofNullable(detail).orElse(status.getReasonPhrase()));
        p.setTitle(Optional.ofNullable(title).orElse(status.getReasonPhrase()));
        p.setInstance(currentRequestUri(req));
        p.setType(URI.create("about:blank"));
        p.setProperty("timestamp", clock.now().toString());
        return p;
    }

    private ResponseEntity<ProblemDetail> respond(HttpStatus status, ProblemDetail body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    // ---- Specific handlers-----------------------------------------------------

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleRse(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ProblemDetail body = pd(status, status.getReasonPhrase(), ex.getReason(), req);
        if (includeStacktrace && status.is5xxServerError())
            body.setProperty("exception", ex.toString());
        return respond(status, body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail body = pd(status, "Validation failed", "One or more fields are invalid.", req);

        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("field", fe.getField());
                    m.put("message", fe.getDefaultMessage());
                    m.put("rejectedValue", fe.getRejectedValue());
                    m.put("code", fe.getCode());
                    return m;
                })
                .toList();
        body.setProperty("errors", errors);
        return respond(status, body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail body = pd(status, "Constraint violation", "Request constraints violated.", req);

        List<Map<String, Object>> errors = ex.getConstraintViolations().stream()
                .map(this::toViolationMap)
                .toList();
        body.setProperty("errors", errors);
        return respond(status, body);
    }

    private Map<String, Object> toViolationMap(ConstraintViolation<?> v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("property", v.getPropertyPath() != null ? v.getPropertyPath().toString() : null);
        m.put("message", v.getMessage());
        m.put("invalidValue", v.getInvalidValue());
        return m;
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail body = pd(status, "Bad request", ex.getMessage(), req);
        return respond(status, body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail body = pd(status, "Invalid argument", ex.getMessage(), req);
        return respond(status, body);
    }

    @ExceptionHandler({ NoSuchElementException.class, EntityNotFoundException.class })
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ProblemDetail body = pd(status, "Not found", ex.getMessage(), req);
        return respond(status, body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ProblemDetail body = pd(status, "Unauthorized", ex.getMessage(), req);
        return respond(status, body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccess(AccessDeniedException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ProblemDetail body = pd(status, "Forbidden", ex.getMessage(), req);
        return respond(status, body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest req) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        ProblemDetail body = pd(status, "Method not allowed", ex.getMessage(), req);
        return respond(status, body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMedia(HttpMediaTypeNotSupportedException ex,
            HttpServletRequest req) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        ProblemDetail body = pd(status, "Unsupported media type", ex.getMessage(), req);
        return respond(status, body);
    }

    // ---- Fallback (last resort)-----------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.toString(), ex);
        ProblemDetail body = pd(status, "Internal Server Error", "An unexpected error occurred.", req);
        if (includeStacktrace)
            body.setProperty("exception", ex.toString());
        return respond(status, body);
    }
}

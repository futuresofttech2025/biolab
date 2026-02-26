package com.biolab.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Centralized exception handler for all BioLab servlet-based microservices.
 * Translates exceptions into consistent {@link ErrorResponse} JSON payloads.
 *
 * <p>Handles: validation errors, not found, access denied, business logic,
 * file upload limits, type mismatches, and unexpected server errors.</p>
 *
 * @author BioLab Engineering Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Jakarta Bean Validation failures (400). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            String field = (e instanceof FieldError fe) ? fe.getField() : e.getObjectName();
            errors.put(field, e.getDefaultMessage());
        });
        log.warn("Validation failed [{}]: {} errors", req.getRequestURI(), errors.size());
        return ResponseEntity.badRequest().body(
            ErrorResponse.of(400, "Validation Failed",
                "One or more fields have validation errors", req.getRequestURI())
                .withValidationErrors(errors));
    }

    /** Missing query/path parameters (400). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
            ErrorResponse.of(400, "Bad Request", ex.getMessage(), req.getRequestURI()));
    }

    /** Type mismatch in path/query params (400). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = String.format("Parameter '%s' must be of type %s", ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return ResponseEntity.badRequest().body(
            ErrorResponse.of(400, "Bad Request", msg, req.getRequestURI()));
    }

    /** Access denied by @PreAuthorize (403). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied [{}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse.of(403, "Forbidden", "You do not have permission to access this resource", req.getRequestURI()));
    }

    /** Resource not found — custom exception (404). */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse.of(404, "Not Found", ex.getMessage(), req.getRequestURI()));
    }

    /** NoSuchElementException from .orElseThrow() (404). */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse.of(404, "Not Found", ex.getMessage() != null ? ex.getMessage() : "Resource not found", req.getRequestURI()));
    }

    /** Business rule violation (400/409/422). */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("Business error [{}]: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(
            ErrorResponse.of(ex.getStatus().value(), ex.getStatus().getReasonPhrase(),
                ex.getMessage(), req.getRequestURI()));
    }

    /** File too large (413). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ErrorResponse.of(413, "Payload Too Large", "File size exceeds the maximum allowed limit", req.getRequestURI()));
    }

    /** IllegalArgumentException (400). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
            ErrorResponse.of(400, "Bad Request", ex.getMessage(), req.getRequestURI()));
    }

    /** Catch-all for unexpected errors (500). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error [{}]: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.of(500, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", req.getRequestURI()));
    }
}

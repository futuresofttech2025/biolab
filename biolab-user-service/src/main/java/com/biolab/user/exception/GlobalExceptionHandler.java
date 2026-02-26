package com.biolab.user.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler — translates exceptions into standardized JSON.
 *
 * @author BioLab Engineering Team
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404).body(ErrorResponse.builder()
                .status(404).error("Not Found").message(ex.getMessage()).path(req.getRequestURI()).build());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        return ResponseEntity.status(409).body(ErrorResponse.builder()
                .status(409).error("Conflict").message(ex.getMessage()).path(req.getRequestURI()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
            errors.put(((FieldError) e).getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status(400).error("Validation Failed").message("Field validation errors")
                .path(req.getRequestURI()).validationErrors(errors).build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status(400).error("Bad Request").message(ex.getMessage()).path(req.getRequestURI()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error [{}]: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(500).body(ErrorResponse.builder()
                .status(500).error("Internal Server Error").message("An unexpected error occurred")
                .path(req.getRequestURI()).build());
    }
}

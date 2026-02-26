package com.biolab.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Standardized JSON error response body used across all BioLab microservices.
 * Follows RFC 7807 Problem Details convention.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private String timestamp;
    private Map<String, String> validationErrors;

    public ErrorResponse() { this.timestamp = Instant.now().toString(); }

    public ErrorResponse(int status, String error, String message, String path) {
        this(); this.status = status; this.error = error; this.message = message; this.path = path;
    }

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path);
    }

    public ErrorResponse withValidationErrors(Map<String, String> errors) {
        this.validationErrors = errors; return this;
    }

    // Getters & Setters
    public int getStatus() { return status; }
    public void setStatus(int s) { this.status = s; }
    public String getError() { return error; }
    public void setError(String e) { this.error = e; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public String getPath() { return path; }
    public void setPath(String p) { this.path = p; }
    public String getTimestamp() { return timestamp; }
    public Map<String, String> getValidationErrors() { return validationErrors; }
}

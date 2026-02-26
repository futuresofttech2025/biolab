package com.biolab.auth.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

/**
 * Custom authentication/authorization exception with HTTP status mapping.
 */
@Getter
public class AuthException extends RuntimeException {
    private final HttpStatus status;
    public AuthException(String message, HttpStatus status) { super(message); this.status = status; }
}

package com.biolab.auth.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

/**
 * Custom authentication / authorization exception with HTTP status mapping.
 *
 * <p>Two constructors are provided:</p>
 * <ul>
 *   <li>{@link #AuthException(String, HttpStatus)} — explicit status (preferred for controllers)</li>
 *   <li>{@link #AuthException(String)} — defaults to {@code 400 Bad Request}
 *       (used in service-layer validation where HTTP semantics are implicit)</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 */
@Getter
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    /** Full constructor — use when the HTTP status is meaningful at the call site. */
    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Convenience constructor — defaults to {@code 400 Bad Request}.
     * Suitable for service-layer validation failures where the caller
     * knows the appropriate HTTP response code.
     */
    public AuthException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }
}

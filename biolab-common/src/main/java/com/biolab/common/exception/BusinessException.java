package com.biolab.common.exception;

import org.springframework.http.HttpStatus;

/**
 * General business rule violation (HTTP 400/409/422).
 */
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(String message) { this(message, HttpStatus.BAD_REQUEST); }
    public BusinessException(String message, HttpStatus status) {
        super(message); this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}

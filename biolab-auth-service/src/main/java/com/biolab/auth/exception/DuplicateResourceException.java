package com.biolab.auth.exception;

/**
 * Thrown when creating a resource that already exists.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String resource, String field, Object value) {
        super(String.format("%s already exists with %s: '%s'", resource, field, value));
    }
}

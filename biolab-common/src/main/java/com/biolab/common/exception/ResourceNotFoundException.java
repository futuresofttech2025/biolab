package com.biolab.common.exception;

/**
 * Thrown when a requested entity is not found (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {
    private final String resource;
    private final String field;
    private final Object value;

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", resource, field, value));
        this.resource = resource; this.field = field; this.value = value;
    }

    public String getResource() { return resource; }
    public String getField() { return field; }
    public Object getValue() { return value; }
}

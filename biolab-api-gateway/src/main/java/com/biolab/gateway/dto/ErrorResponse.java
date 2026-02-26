package com.biolab.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standardized error response returned by the API Gateway.
 *
 * <p>All error responses from the gateway follow this structure for
 * consistent client-side error handling across the platform.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** HTTP status code (e.g., 401, 503) */
    private int status;

    /** Error category (e.g., "UNAUTHORIZED", "SERVICE_UNAVAILABLE") */
    private String error;

    /** Human-readable error description */
    private String message;

    /** Request path that triggered the error */
    private String path;

    /** ISO-8601 timestamp of the error occurrence */
    @Builder.Default
    private String timestamp = Instant.now().toString();
}

package com.biolab.user.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.Instant;
import java.util.Map;

/** Standardized error response body. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    @Builder.Default private String timestamp = Instant.now().toString();
    private Map<String, String> validationErrors;
}

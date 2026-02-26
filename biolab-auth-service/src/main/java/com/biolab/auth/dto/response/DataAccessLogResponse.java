package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Data access log entry response. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DataAccessLogResponse {
    private UUID id;
    private UUID userId;
    private String resourceType;
    private UUID resourceId;
    private String action;
    private String ipAddress;
    private Instant createdAt;
}

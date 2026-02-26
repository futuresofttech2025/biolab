package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Active session response. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSessionResponse {
    private UUID id;
    private String ipAddress;
    private String userAgent;
    private Boolean isActive;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant lastAccessedAt;
}

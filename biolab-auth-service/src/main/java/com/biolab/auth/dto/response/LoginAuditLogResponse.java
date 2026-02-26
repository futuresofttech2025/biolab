package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Audit log entry response. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginAuditLogResponse {
    private UUID id;
    private UUID userId;
    private String email;
    private String ipAddress;
    private String action;
    private String status;
    private Boolean mfaUsed;
    private String failureReason;
    private Instant createdAt;
}

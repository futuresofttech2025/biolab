package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** User-role assignment response. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRoleResponse {
    private UUID id;
    private UUID userId;
    private RoleResponse role;
    private UUID assignedBy;
    private Instant assignedAt;
    private Instant expiresAt;
}

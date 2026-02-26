package com.biolab.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Assign a role to a user. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRoleAssignRequest {
    @NotNull private UUID roleId;
    private Instant expiresAt;
}

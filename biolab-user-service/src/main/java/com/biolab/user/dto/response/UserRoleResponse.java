package com.biolab.user.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * User-role assignment response.
 *
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRoleResponse {

    private UUID id;
    private UUID userId;
    private String roleName;
    private String roleDisplayName;
    private UUID assignedBy;
    private Instant assignedAt;
    private Instant expiresAt;
}

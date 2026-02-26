package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Role details response. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleResponse {
    private UUID id;
    private String name;
    private String displayName;
    private String description;
    private Boolean isSystemRole;
    private Instant createdAt;
}

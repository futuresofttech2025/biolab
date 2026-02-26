package com.biolab.user.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Role details response — includes associated permission names.
 *
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleResponse {

    private UUID id;
    private String name;
    private String displayName;
    private String description;
    private Boolean isSystemRole;
    private Instant createdAt;

    /** Permission names assigned to this role. */
    private List<String> permissions;
}

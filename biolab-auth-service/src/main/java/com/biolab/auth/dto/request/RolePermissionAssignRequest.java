package com.biolab.auth.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;
import java.util.UUID;

/** Assign permissions to a role. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermissionAssignRequest {
    @NotEmpty private List<UUID> permissionIds;
}

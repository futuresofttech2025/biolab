package com.biolab.auth.dto.response;

import lombok.*;
import java.util.List;
import java.util.UUID;

/** Role with its assigned permissions. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermissionResponse {
    private UUID roleId;
    private String roleName;
    private List<PermissionResponse> permissions;
}

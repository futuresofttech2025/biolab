package com.biolab.auth.service;

import com.biolab.auth.dto.request.RolePermissionAssignRequest;
import com.biolab.auth.dto.response.RolePermissionResponse;
import java.util.UUID;

/** CRUD for role_permissions junction. */
public interface RolePermissionService {
    RolePermissionResponse assignPermissions(UUID roleId, RolePermissionAssignRequest request);
    RolePermissionResponse getByRoleId(UUID roleId);
    void revokePermission(UUID roleId, UUID permissionId);
}

package com.biolab.auth.service;

import com.biolab.auth.dto.request.UserRoleAssignRequest;
import com.biolab.auth.dto.response.UserRoleResponse;
import java.util.List;
import java.util.UUID;

/** CRUD for user_roles junction. */
public interface UserRoleService {
    UserRoleResponse assign(UUID userId, UserRoleAssignRequest request, UUID assignedBy);
    List<UserRoleResponse> getByUserId(UUID userId);
    List<UserRoleResponse> getByRoleId(UUID roleId);
    void revoke(UUID userId, UUID roleId);
}

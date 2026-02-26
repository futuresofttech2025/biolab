package com.biolab.user.service;

import com.biolab.user.dto.response.RoleResponse;
import com.biolab.user.dto.response.UserRoleResponse;

import java.util.List;
import java.util.UUID;

/**
 * Read-only service for querying user roles and role details.
 * Role assignment/revocation is handled by the Auth Service.
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
public interface UserRoleQueryService {

    /** Lists all roles assigned to a user. */
    List<UserRoleResponse> getRolesByUserId(UUID userId);

    /** Lists all role names for a user (for JWT claims). */
    List<String> getRoleNamesByUserId(UUID userId);

    /** Retrieves a role by UUID with associated permissions. */
    RoleResponse getRoleById(UUID roleId);

    /** Lists all available roles. */
    List<RoleResponse> getAllRoles();

    /** Checks if a user has a specific role. */
    boolean hasRole(UUID userId, String roleName);
}

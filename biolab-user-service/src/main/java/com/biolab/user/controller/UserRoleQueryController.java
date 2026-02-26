package com.biolab.user.controller;

import com.biolab.user.dto.response.RoleResponse;
import com.biolab.user.dto.response.UserRoleResponse;
import com.biolab.user.service.UserRoleQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Role &amp; Permission Query Controller (Read-Only).
 *
 * <h3>Endpoints (Slide 13):</h3>
 * <ul>
 *   <li>{@code GET /api/roles} — List all roles with permissions (Admin)</li>
 *   <li>{@code GET /api/roles/{roleId}} — Get role detail (Admin)</li>
 *   <li>{@code GET /api/users/{userId}/roles} — Get user's roles (Admin or self)</li>
 *   <li>{@code GET /api/users/{userId}/roles/check} — Check if user has role</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Roles & Permissions", description = "Read-only role and permission queries")
public class UserRoleQueryController {

    private final UserRoleQueryService userRoleQueryService;

    @GetMapping("/api/roles")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "List all roles with permissions (Admin)")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(userRoleQueryService.getAllRoles());
    }

    @GetMapping("/api/roles/{roleId}")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Get role by ID with permissions (Admin)")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable UUID roleId) {
        return ResponseEntity.ok(userRoleQueryService.getRoleById(roleId));
    }

    @GetMapping("/api/users/{userId}/roles")
    @PreAuthorize("@perm.isOwnerOrAdmin(#userId)")
    @Operation(summary = "Get user's assigned roles",
               description = "Admin can view any user's roles; users can view their own.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Roles returned"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<List<UserRoleResponse>> getUserRoles(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        return ResponseEntity.ok(userRoleQueryService.getRolesByUserId(userId));
    }

    @GetMapping("/api/users/{userId}/roles/check")
    @PreAuthorize("@perm.isOwnerOrAdmin(#userId)")
    @Operation(summary = "Check if user has a specific role")
    public ResponseEntity<Boolean> checkUserRole(
            @PathVariable UUID userId,
            @Parameter(description = "Role name to check") @RequestParam String roleName) {
        return ResponseEntity.ok(userRoleQueryService.hasRole(userId, roleName));
    }
}

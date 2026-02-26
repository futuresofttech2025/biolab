package com.biolab.auth.controller;

import com.biolab.auth.dto.request.RolePermissionAssignRequest;
import com.biolab.auth.dto.response.RolePermissionResponse;
import com.biolab.auth.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for role-permission mapping CRUD.
 *
 * <h3>Base Path:</h3> {@code /api/roles/{roleId}/permissions}
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/roles/{roleId}/permissions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Role Permissions", description = "Assign and revoke granular permissions for roles")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    /** Assigns permissions to a role (idempotent — skips existing assignments). */
    @PostMapping
    @Operation(summary = "Assign permissions to role", description = "Idempotent — existing assignments are skipped")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permissions assigned"),
        @ApiResponse(responseCode = "404", description = "Role or permission not found")
    })
    public ResponseEntity<RolePermissionResponse> assignPermissions(
            @Parameter(description = "Role UUID") @PathVariable UUID roleId,
            @Valid @RequestBody RolePermissionAssignRequest request) {
        log.info("Assigning {} permissions to role {}", request.getPermissionIds().size(), roleId);
        return ResponseEntity.ok(rolePermissionService.assignPermissions(roleId, request));
    }

    /** Lists all permissions assigned to a role. */
    @GetMapping
    @Operation(summary = "List role's permissions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permissions retrieved"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<RolePermissionResponse> getByRole(
            @Parameter(description = "Role UUID") @PathVariable UUID roleId) {
        return ResponseEntity.ok(rolePermissionService.getByRoleId(roleId));
    }

    /** Revokes a specific permission from a role. */
    @DeleteMapping("/{permId}")
    @Operation(summary = "Revoke permission from role")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Permission revoked"),
        @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<Void> revokePermission(
            @Parameter(description = "Role UUID") @PathVariable UUID roleId,
            @Parameter(description = "Permission UUID") @PathVariable UUID permId) {
        log.info("Revoking permission {} from role {}", permId, roleId);
        rolePermissionService.revokePermission(roleId, permId);
        return ResponseEntity.noContent().build();
    }
}

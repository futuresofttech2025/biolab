package com.biolab.auth.controller;

import com.biolab.auth.dto.request.UserRoleAssignRequest;
import com.biolab.auth.dto.response.UserRoleResponse;
import com.biolab.auth.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user-role assignment CRUD.
 *
 * <h3>Base Path:</h3> {@code /api/users/{userId}/roles}
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/users/{userId}/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Roles", description = "Assign and revoke RBAC roles for users")
public class UserRoleController {

    private final UserRoleService userRoleService;

    /** Assigns a role to a user with optional expiry. */
    @PostMapping
    @Operation(summary = "Assign role to user", description = "Assigns a role with optional expiry date")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Role assigned"),
        @ApiResponse(responseCode = "404", description = "User or role not found"),
        @ApiResponse(responseCode = "409", description = "User already has this role")
    })
    public ResponseEntity<UserRoleResponse> assign(
            @Parameter(description = "Target user UUID") @PathVariable UUID userId,
            @Valid @RequestBody UserRoleAssignRequest request,
            @Parameter(description = "Assigning admin UUID (from gateway)")
            @RequestHeader(value = "X-User-Id", required = false) String assignedBy) {

        UUID assignedByUuid = assignedBy != null ? UUID.fromString(assignedBy) : null;
        log.info("Assigning role {} to user {} by {}", request.getRoleId(), userId, assignedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userRoleService.assign(userId, request, assignedByUuid));
    }

    /** Lists all roles assigned to a user. */
    @GetMapping
    @Operation(summary = "List user's roles")
    @ApiResponse(responseCode = "200", description = "Roles retrieved")
    public ResponseEntity<List<UserRoleResponse>> getByUser(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        return ResponseEntity.ok(userRoleService.getByUserId(userId));
    }

    /** Revokes a role from a user. */
    @DeleteMapping("/{roleId}")
    @Operation(summary = "Revoke role from user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Role revoked"),
        @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<Void> revoke(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Parameter(description = "Role UUID") @PathVariable UUID roleId) {
        log.info("Revoking role {} from user {}", roleId, userId);
        userRoleService.revoke(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}

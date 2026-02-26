package com.biolab.user.controller;

import com.biolab.user.dto.request.UserOrganizationAssignRequest;
import com.biolab.user.dto.response.UserOrganizationResponse;
import com.biolab.user.service.UserOrganizationService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * User-Organization Membership Controller.
 *
 * <h3>Endpoints (Slide 13):</h3>
 * <ul>
 *   <li>{@code POST /api/users/{userId}/organizations} — Add member (Admin)</li>
 *   <li>{@code GET /api/users/{userId}/organizations} — List user's orgs</li>
 *   <li>{@code PUT /api/users/{userId}/organizations/{orgId}} — Update membership (Admin)</li>
 *   <li>{@code DELETE /api/users/{userId}/organizations/{orgId}} — Remove (Admin)</li>
 *   <li>{@code GET /api/organizations/{orgId}/members} — List org members (Admin)</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User-Organization", description = "Membership management with RBAC controls")
public class UserOrganizationController {

    private final UserOrganizationService userOrganizationService;

    @PostMapping("/api/users/{userId}/organizations")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Add user to organization (Admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Member added"),
        @ApiResponse(responseCode = "403", description = "Admin access required"),
        @ApiResponse(responseCode = "409", description = "Already a member")
    })
    public ResponseEntity<UserOrganizationResponse> addMember(
            @PathVariable UUID userId,
            @Valid @RequestBody UserOrganizationAssignRequest request) {
        log.info("POST /api/users/{}/organizations — orgId={}", userId, request.getOrganizationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userOrganizationService.addMember(userId, request));
    }

    @GetMapping("/api/users/{userId}/organizations")
    @PreAuthorize("@perm.isOwnerOrAdmin(#userId)")
    @Operation(summary = "List user's organizations")
    public ResponseEntity<List<UserOrganizationResponse>> getUserOrganizations(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        return ResponseEntity.ok(userOrganizationService.getByUserId(userId));
    }

    @PutMapping("/api/users/{userId}/organizations/{orgId}")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Update user's organization membership (Admin)")
    public ResponseEntity<UserOrganizationResponse> updateMembership(
            @PathVariable UUID userId, @PathVariable UUID orgId,
            @Valid @RequestBody UserOrganizationAssignRequest request) {
        log.info("PUT /api/users/{}/organizations/{}", userId, orgId);
        return ResponseEntity.ok(userOrganizationService.updateMembership(userId, orgId, request.getRoleInOrg(), request.getIsPrimary()));
    }

    @DeleteMapping("/api/users/{userId}/organizations/{orgId}")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Remove user from organization (Admin)")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID userId, @PathVariable UUID orgId) {
        log.info("DELETE /api/users/{}/organizations/{}", userId, orgId);
        userOrganizationService.removeMember(userId, orgId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/organizations/{orgId}/members")
    @PreAuthorize("@perm.isAdminOrOrgMember(#orgId.toString())")
    @Operation(summary = "List organization members")
    public ResponseEntity<List<UserOrganizationResponse>> getOrgMembers(
            @Parameter(description = "Organization UUID") @PathVariable UUID orgId) {
        return ResponseEntity.ok(userOrganizationService.getByOrganizationId(orgId));
    }
}

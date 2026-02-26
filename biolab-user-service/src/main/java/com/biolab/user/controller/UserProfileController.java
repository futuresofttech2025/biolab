package com.biolab.user.controller;

import com.biolab.common.security.CurrentUser;
import com.biolab.common.security.CurrentUserContext;
import com.biolab.user.dto.request.UserUpdateRequest;
import com.biolab.user.dto.response.PageResponse;
import com.biolab.user.dto.response.UserProfileResponse;
import com.biolab.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User Profile Management Controller.
 *
 * <h3>Endpoints (Slide 13 — LLD User Service):</h3>
 * <ul>
 *   <li>{@code GET /api/users/me} — Current user profile (any authenticated)</li>
 *   <li>{@code GET /api/users/{id}} — Get user by ID (admin or self)</li>
 *   <li>{@code GET /api/users/email/{email}} — Get by email (admin only)</li>
 *   <li>{@code GET /api/users} — Search users, paginated (admin only — Slide 6)</li>
 *   <li>{@code PUT /api/users/{id}} — Update profile (admin or self)</li>
 *   <li>{@code DELETE /api/users/{id}} — Soft delete (admin only)</li>
 *   <li>{@code POST /api/users/{id}/reactivate} — Reactivate user (admin only)</li>
 * </ul>
 *
 * <h3>RBAC (Slide 6):</h3>
 * <p>USER_VIEW_ALL and USER_MANAGE restricted to SUPER_ADMIN and ADMIN.
 * All users can view/update their own profile (SETTINGS_OWN_PROFILE).</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profiles", description = "User profile CRUD with RBAC-protected access")
public class UserProfileController {

    private final UserProfileService userProfileService;

    // ─── GET /api/users/me ──────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get current user profile",
               description = "Returns the authenticated user's full profile including roles and organizations.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        CurrentUser current = CurrentUserContext.require();
        log.debug("GET /api/users/me — userId={}", current.userId());
        UserProfileResponse profile = userProfileService.getById(current.userId());
        return ResponseEntity.ok(profile);
    }

    // ─── GET /api/users/{id} ────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("@perm.isOwnerOrAdmin(#id)")
    @Operation(summary = "Get user by ID",
               description = "Returns user profile. Accessible by the user themselves or admins (USER_VIEW).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> getUserById(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        log.debug("GET /api/users/{} — requested by {}", id, getCurrentUserId());
        return ResponseEntity.ok(userProfileService.getById(id));
    }

    // ─── GET /api/users/email/{email} ───────────────────────────────

    @GetMapping("/email/{email}")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Get user by email (Admin)",
               description = "Lookup user by email address. Restricted to SUPER_ADMIN and ADMIN roles.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "403", description = "Admin access required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> getUserByEmail(
            @Parameter(description = "User email") @PathVariable String email) {
        log.debug("GET /api/users/email/{} — admin lookup", email);
        return ResponseEntity.ok(userProfileService.getByEmail(email));
    }

    // ─── GET /api/users (Admin — Paginated Search) ──────────────────

    @GetMapping
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Search users (Admin)",
               description = "Paginated user search with keyword filter. Restricted to SUPER_ADMIN/ADMIN (USER_VIEW_ALL).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results returned"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<PageResponse<UserProfileResponse>> searchUsers(
            @Parameter(description = "Search keyword (name, email)") @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /api/users — keyword={} isActive={}", keyword, isActive);
        return ResponseEntity.ok(userProfileService.search(keyword, isActive, pageable));
    }

    // ─── PUT /api/users/{id} ────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("@perm.isOwnerOrAdmin(#id)")
    @Operation(summary = "Update user profile",
               description = "Partial update. Users can update own profile; admins can update any profile.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> updateUser(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("PUT /api/users/{} — by {}", id, getCurrentUserId());
        return ResponseEntity.ok(userProfileService.update(id, request));
    }

    // ─── DELETE /api/users/{id} (Soft Delete) ───────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Deactivate user (Admin)",
               description = "Soft delete — sets is_active=false. Records retained for HIPAA compliance.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deactivated"),
        @ApiResponse(responseCode = "403", description = "Admin access required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        log.info("DELETE /api/users/{} — soft delete by {}", id, getCurrentUserId());
        userProfileService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // ─── POST /api/users/{id}/reactivate ────────────────────────────

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Reactivate user (Admin)",
               description = "Re-enables a previously deactivated user account.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User reactivated"),
        @ApiResponse(responseCode = "403", description = "Admin access required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileResponse> reactivateUser(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        log.info("POST /api/users/{}/reactivate — by {}", id, getCurrentUserId());
        return ResponseEntity.ok(userProfileService.reactivate(id));
    }

    // ─── Helper ─────────────────────────────────────────────────────

    private String getCurrentUserId() {
        return CurrentUserContext.get().map(u -> u.userId().toString()).orElse("anonymous");
    }
}

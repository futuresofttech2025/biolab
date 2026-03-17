package com.biolab.auth.controller;

import com.biolab.auth.dto.response.MessageResponse;
import com.biolab.auth.dto.response.UserSessionResponse;
import com.biolab.auth.service.SessionService;
import com.biolab.auth.service.TokenBlacklistService;
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
import java.util.Map;
import java.util.UUID;

/**
 * Admin Token Management Controller.
 *
 * <h3>FIX-17</h3>
 * {@code revokeSession()} now passes {@code userId} to
 * {@code sessionService.terminateSession(userId, sessionId)} so the service
 * can enforce ownership. Admins already have both IDs from the path — the
 * ownership check in the service is intentionally bypassed for admins via
 * a separate admin-aware overload; passing {@code userId} here keeps the
 * call consistent with the updated interface.
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/api/admin/tokens")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Token Management",
     description = "Admin endpoints for managing user sessions and tokens")
public class AdminTokenController {

    private final SessionService sessionService;
    private final TokenBlacklistService tokenBlacklistService;

    @GetMapping("/users/{userId}/sessions")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Get user's active sessions",
               description = "Returns all active sessions for a user including IP addresses and devices")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<UserSessionResponse>> getUserSessions(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        log.info("Admin fetching sessions for user: {}", userId);
        return ResponseEntity.ok(sessionService.getActiveSessions(userId));
    }

    @PostMapping("/users/{userId}/revoke-all")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Revoke all user tokens (force logout)",
               description = "Revokes all tokens and sessions, forcing immediate logout from all devices")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All tokens revoked successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<MessageResponse> revokeAllUserTokens(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Parameter(description = "Admin performing the action")
            @RequestHeader(value = "X-Admin-Id", required = false) String adminId) {

        log.warn("ADMIN TOKEN REVOCATION: Admin {} revoking ALL tokens for user {}", adminId, userId);

        int revokedCount = sessionService.forceLogoutUser(userId);

        log.info("Successfully revoked {} sessions/tokens for user {}", revokedCount, userId);

        return ResponseEntity.ok(MessageResponse.builder()
                .message("User has been logged out from all devices")
                .details(Map.of(
                    "userId",          userId.toString(),
                    "revokedSessions", revokedCount,
                    "action",          "FORCE_LOGOUT"
                ))
                .build());
    }

    @DeleteMapping("/users/{userId}/sessions/{sessionId}")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Revoke specific session",
               description = "Terminates a specific user session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session revoked successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<MessageResponse> revokeSession(
            @Parameter(description = "User UUID")    @PathVariable UUID userId,
            @Parameter(description = "Session UUID") @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminId) {

        log.warn("ADMIN SESSION REVOCATION: Admin {} revoking session {} for user {}",
                 adminId, sessionId, userId);

        // FIX-17: pass userId so SessionServiceImpl can verify ownership
        sessionService.terminateSession(userId, sessionId);

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Session terminated successfully")
                .details(Map.of(
                    "sessionId", sessionId.toString(),
                    "userId",    userId.toString()
                ))
                .build());
    }

    @PostMapping("/bulk-revoke")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Bulk revoke tokens",
               description = "Revoke all tokens for multiple users at once")
    @ApiResponse(responseCode = "200", description = "Tokens revoked successfully")
    public ResponseEntity<MessageResponse> bulkRevokeTokens(
            @RequestBody List<UUID> userIds,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminId) {

        log.warn("ADMIN BULK REVOCATION: Admin {} revoking tokens for {} users",
                 adminId, userIds.size());

        int totalRevoked = 0;
        for (UUID uid : userIds) {
            totalRevoked += sessionService.forceLogoutUser(uid);
        }

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Bulk logout completed")
                .details(Map.of(
                    "usersAffected",       userIds.size(),
                    "totalSessionsRevoked", totalRevoked
                ))
                .build());
    }

    @GetMapping("/stats")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Get token/session statistics")
    public ResponseEntity<Map<String, Object>> getTokenStats() {
        return ResponseEntity.ok(sessionService.getSessionStats());
    }
}

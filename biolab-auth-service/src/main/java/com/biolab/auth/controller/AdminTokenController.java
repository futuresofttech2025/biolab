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
 * Admin Token Management Controller
 * 
 * <h3>Features:</h3>
 * <ul>
 *   <li>View active sessions for any user</li>
 *   <li>Revoke all tokens for a specific user (force logout)</li>
 *   <li>Revoke specific session</li>
 *   <li>Bulk revoke sessions for multiple users</li>
 * </ul>
 * 
 * <p>All endpoints require ADMIN or SUPER_ADMIN role.</p>
 * 
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/admin/tokens")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Token Management", description = "Admin endpoints for managing user sessions and tokens")
public class AdminTokenController {

    private final SessionService sessionService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Get all active sessions for a specific user.
     * 
     * @param userId The user's UUID
     * @return List of active sessions with IP, user agent, timestamps
     */
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

    /**
     * Revoke ALL tokens for a user - forces immediate logout from all devices.
     * 
     * <p>This will:</p>
     * <ul>
     *   <li>Revoke all refresh tokens for the user</li>
     *   <li>Invalidate all active sessions</li>
     *   <li>The user will be logged out on their next API call</li>
     * </ul>
     * 
     * @param userId The user's UUID
     * @return Success message with count of revoked sessions
     */
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
        
        log.warn("🔒 ADMIN TOKEN REVOCATION: Admin {} revoking ALL tokens for user {}", adminId, userId);
        
        int revokedCount = sessionService.forceLogoutUser(userId);
        
        log.info("Successfully revoked {} sessions/tokens for user {}", revokedCount, userId);
        
        return ResponseEntity.ok(MessageResponse.builder()
                .message("User has been logged out from all devices")
                .details(Map.of(
                    "userId", userId.toString(),
                    "revokedSessions", revokedCount,
                    "action", "FORCE_LOGOUT"
                ))
                .build());
    }

    /**
     * Revoke a specific session for a user.
     * 
     * @param userId The user's UUID
     * @param sessionId The session UUID to revoke
     * @return Success message
     */
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
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Parameter(description = "Session UUID") @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminId) {
        
        log.warn("🔒 ADMIN SESSION REVOCATION: Admin {} revoking session {} for user {}", 
                 adminId, sessionId, userId);
        
        sessionService.terminateSession(sessionId);
        
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Session terminated successfully")
                .details(Map.of(
                    "sessionId", sessionId.toString(),
                    "userId", userId.toString()
                ))
                .build());
    }

    /**
     * Bulk revoke tokens for multiple users.
     * 
     * @param userIds List of user UUIDs
     * @return Success message with count
     */
    @PostMapping("/bulk-revoke")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Bulk revoke tokens", 
               description = "Revoke all tokens for multiple users at once")
    @ApiResponse(responseCode = "200", description = "Tokens revoked successfully")
    public ResponseEntity<MessageResponse> bulkRevokeTokens(
            @RequestBody List<UUID> userIds,
            @RequestHeader(value = "X-Admin-Id", required = false) String adminId) {
        
        log.warn("🔒 ADMIN BULK REVOCATION: Admin {} revoking tokens for {} users", 
                 adminId, userIds.size());
        
        int totalRevoked = 0;
        for (UUID userId : userIds) {
            totalRevoked += sessionService.forceLogoutUser(userId);
        }
        
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Bulk logout completed")
                .details(Map.of(
                    "usersAffected", userIds.size(),
                    "totalSessionsRevoked", totalRevoked
                ))
                .build());
    }

    /**
     * Get count of active sessions across all users (for dashboard).
     */
    @GetMapping("/stats")
    @PreAuthorize("@perm.isAdmin()")
    @Operation(summary = "Get token/session statistics")
    public ResponseEntity<Map<String, Object>> getTokenStats() {
        return ResponseEntity.ok(sessionService.getSessionStats());
    }
}

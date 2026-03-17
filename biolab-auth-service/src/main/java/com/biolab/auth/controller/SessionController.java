package com.biolab.auth.controller;

import com.biolab.auth.dto.response.UserSessionResponse;
import com.biolab.auth.exception.AuthException;
import com.biolab.auth.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user session management.
 *
 * <h3>Fixes applied</h3>
 * <ul>
 *   <li>FIX-6:  ownership check — users may only manage their own sessions;
 *       ADMIN/SUPER_ADMIN may manage any user's sessions</li>
 *   <li>FIX-17: terminateSession() now passes {@code userId} to the service
 *       layer so it can verify the session belongs to that user</li>
 *   <li>SESSION-FIX: base path moved from {@code /api/users} to
 *       {@code /api/auth/users} so the API Gateway correctly routes requests
 *       to AUTH-SERVICE instead of USER-SERVICE.</li>
 * </ul>
 *
 * <h3>Base path</h3> {@code /api/auth/users/{userId}/sessions}
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/api/auth/users/{userId}/sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sessions", description = "View and terminate active user sessions")
public class SessionController {

    private final SessionService sessionService;

    /** Lists all active sessions for a user (IP, user agent, timestamps). */
    @GetMapping
    @Operation(summary = "List active sessions",
            description = "Returns active sessions with IP/UA details.")
    @ApiResponse(responseCode = "200", description = "Sessions retrieved")
    public ResponseEntity<List<UserSessionResponse>> getActiveSessions(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @RequestHeader("X-User-Id")    String callerId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String callerRoles) {

        enforceOwnership(callerId, callerRoles, userId);   // FIX-6
        return ResponseEntity.ok(sessionService.getActiveSessions(userId));
    }

    /**
     * Terminates a specific session.
     * FIX-17: passes {@code userId} to service so it can verify ownership of the session.
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Terminate session")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Session terminated"),
            @ApiResponse(responseCode = "403", description = "Session does not belong to this user"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<Void> terminateSession(
            @Parameter(description = "User UUID")    @PathVariable UUID userId,
            @Parameter(description = "Session UUID") @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id")    String callerId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String callerRoles) {

        enforceOwnership(callerId, callerRoles, userId);       // FIX-6
        log.info("Terminating session {} for user {}", sessionId, userId);
        sessionService.terminateSession(userId, sessionId);    // FIX-17: pass userId
        return ResponseEntity.noContent().build();
    }

    /** Force logout from all devices — terminates ALL active sessions. */
    @DeleteMapping
    @Operation(summary = "Terminate all sessions",
            description = "Force logout from all devices.")
    @ApiResponse(responseCode = "204", description = "All sessions terminated")
    public ResponseEntity<Void> terminateAllSessions(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @RequestHeader("X-User-Id")    String callerId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String callerRoles) {

        enforceOwnership(callerId, callerRoles, userId);   // FIX-6
        log.info("Terminating ALL sessions for user {}", userId);
        sessionService.terminateAllSessions(userId);
        return ResponseEntity.noContent().build();
    }

    private void enforceOwnership(String callerId, String callerRoles, UUID targetUserId) {
        boolean isOwner = callerId.equals(targetUserId.toString());
        boolean isAdmin = callerRoles.contains("ADMIN") || callerRoles.contains("SUPER_ADMIN");
        if (!isOwner && !isAdmin) {
            throw new AuthException(
                    "You are not permitted to manage sessions for another user.",
                    HttpStatus.FORBIDDEN);
        }
    }
}
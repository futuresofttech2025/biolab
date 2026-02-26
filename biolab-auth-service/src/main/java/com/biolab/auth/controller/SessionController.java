package com.biolab.auth.controller;

import com.biolab.auth.dto.response.UserSessionResponse;
import com.biolab.auth.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user session management.
 *
 * <h3>Base Path:</h3> {@code /api/users/{userId}/sessions}
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/users/{userId}/sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sessions", description = "View and terminate active user sessions")
public class SessionController {

    private final SessionService sessionService;

    /** Lists all active sessions for a user (IP, user agent, timestamps). */
    @GetMapping
    @Operation(summary = "List active sessions", description = "Returns active sessions with IP/UA details")
    @ApiResponse(responseCode = "200", description = "Sessions retrieved")
    public ResponseEntity<List<UserSessionResponse>> getActiveSessions(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        return ResponseEntity.ok(sessionService.getActiveSessions(userId));
    }

    /** Terminates a specific session. */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Terminate session")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session terminated"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<Void> terminateSession(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Parameter(description = "Session UUID") @PathVariable UUID sessionId) {
        log.info("Terminating session {} for user {}", sessionId, userId);
        sessionService.terminateSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    /** Force logout from all devices — terminates ALL active sessions. */
    @DeleteMapping
    @Operation(summary = "Terminate all sessions", description = "Force logout from all devices")
    @ApiResponse(responseCode = "204", description = "All sessions terminated")
    public ResponseEntity<Void> terminateAllSessions(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        log.info("Terminating ALL sessions for user {}", userId);
        sessionService.terminateAllSessions(userId);
        return ResponseEntity.noContent().build();
    }
}

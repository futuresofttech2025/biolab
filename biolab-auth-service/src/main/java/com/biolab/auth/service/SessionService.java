package com.biolab.auth.service;

import com.biolab.auth.dto.response.UserSessionResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contract for user session management.
 *
 * <h3>FIX-17</h3>
 * <p>{@link #terminateSession(UUID, UUID)} now accepts {@code userId} so the
 * implementation can verify the session belongs to the requesting user before
 * deactivating it, preventing cross-user session termination.</p>
 *
 * @author BioLab Engineering Team
 */
public interface SessionService {

    List<UserSessionResponse> getActiveSessions(UUID userId);

    /**
     * Terminates a specific session.
     * FIX-17: implementation must verify {@code session.getUser().getId().equals(userId)}.
     *
     * @param userId    the user who owns the session
     * @param sessionId the session to terminate
     * @throws com.biolab.auth.exception.ResourceNotFoundException if not found
     * @throws com.biolab.auth.exception.AuthException             if session belongs to different user
     */
    void terminateSession(UUID userId, UUID sessionId);

    void terminateAllSessions(UUID userId);

    int forceLogoutUser(UUID userId);

    Map<String, Object> getSessionStats();
}

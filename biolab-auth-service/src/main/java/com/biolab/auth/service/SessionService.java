package com.biolab.auth.service;

import com.biolab.auth.dto.response.UserSessionResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing user sessions and tokens.
 * 
 * @author BioLab Engineering Team
 */
public interface SessionService {
    
    /**
     * Get all active sessions for a user.
     */
    List<UserSessionResponse> getActiveSessions(UUID userId);
    
    /**
     * Terminate a specific session.
     */
    void terminateSession(UUID sessionId);
    
    /**
     * Terminate all sessions for a user.
     */
    void terminateAllSessions(UUID userId);
    
    /**
     * Force logout a user by revoking all their tokens and sessions.
     * This is used by admins to immediately log out a user from all devices.
     * 
     * @param userId The user to force logout
     * @return Number of sessions/tokens revoked
     */
    int forceLogoutUser(UUID userId);
    
    /**
     * Get statistics about active sessions across the platform.
     * Used for admin dashboard.
     */
    Map<String, Object> getSessionStats();
}

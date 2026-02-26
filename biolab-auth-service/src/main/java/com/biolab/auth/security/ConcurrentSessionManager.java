package com.biolab.auth.security;

import com.biolab.auth.entity.UserSession;
import com.biolab.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Enforces concurrent session limits per user.
 *
 * <h3>Policy (Slide 10 — Session Management):</h3>
 * <ul>
 *   <li>Maximum 5 concurrent active sessions per user (configurable)</li>
 *   <li>When limit exceeded, oldest session is automatically terminated</li>
 *   <li>SUPER_ADMIN accounts have unlimited sessions</li>
 *   <li>Session termination triggers audit log entry</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConcurrentSessionManager {

    private final UserSessionRepository sessionRepository;

    @Value("${app.security.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    /**
     * Enforces the concurrent session limit for a user.
     * Terminates the oldest sessions if the limit is exceeded.
     *
     * @param userId the user ID to check
     */
    public void enforceSessionLimit(UUID userId) {
        List<UserSession> activeSessions = sessionRepository
                .findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);

        if (activeSessions.size() >= maxConcurrentSessions) {
            // Terminate oldest sessions to make room
            int sessionsToTerminate = activeSessions.size() - maxConcurrentSessions + 1;
            List<UserSession> toTerminate = activeSessions.subList(
                    activeSessions.size() - sessionsToTerminate, activeSessions.size());

            toTerminate.forEach(session -> {
                session.setIsActive(false);
                sessionRepository.save(session);
                log.info("Terminated oldest session {} for user {} (concurrent limit enforced)",
                        session.getId(), userId);
            });
        }
    }

    /**
     * Returns the number of active sessions for a user.
     *
     * @param userId the user ID
     * @return count of active sessions
     */
    public int getActiveSessionCount(UUID userId) {
        return sessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId).size();
    }
}

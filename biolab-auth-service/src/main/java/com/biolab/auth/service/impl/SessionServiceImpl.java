package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.UserSessionResponse;
import com.biolab.auth.entity.UserSession;
import com.biolab.auth.entity.enums.RevokedReason;
import com.biolab.auth.exception.AuthException;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.RefreshTokenRepository;
import com.biolab.auth.repository.UserSessionRepository;
import com.biolab.auth.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Session service implementation.
 *
 * <h3>Fixes applied</h3>
 * <ul>
 *   <li>FIX-17: {@link #terminateSession(UUID, UUID)} now verifies the session belongs
 *       to the given user before deactivating it, preventing cross-user session kill.</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository sessionRepo;
    private final RefreshTokenRepository refreshTokenRepo;

    @Override
    @Transactional(readOnly = true)
    public List<UserSessionResponse> getActiveSessions(UUID userId) {
        log.debug("Fetching active sessions for user: {}", userId);
        return sessionRepo.findByUserIdAndIsActiveTrue(userId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * FIX-17: verifies session belongs to {@code userId} before deactivating.
     * Prevents authenticated user A from terminating user B's sessions by
     * guessing or enumerating session UUIDs.
     */
    @Override
    public void terminateSession(UUID userId, UUID sessionId) {
        log.info("Terminating session {} for user {}", sessionId, userId);

        UserSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

        // FIX-17: ownership check — session must belong to the requesting user
        if (!session.getUser().getId().equals(userId)) {
            log.warn("Ownership violation: user {} attempted to terminate session {} owned by {}",
                    userId, sessionId, session.getUser().getId());
            throw new AuthException(
                    "Session does not belong to this user.", HttpStatus.FORBIDDEN);
        }

        session.setIsActive(false);
        sessionRepo.save(session);
    }

    @Override
    public void terminateAllSessions(UUID userId) {
        log.info("Terminating all sessions for user: {}", userId);
        sessionRepo.deactivateAllUserSessions(userId);
    }

    /**
     * Force logout: revoke all refresh tokens + deactivate all sessions.
     * Used by admins for immediate account lockdown.
     */
    @Override
    public int forceLogoutUser(UUID userId) {
        log.warn("FORCE LOGOUT: revoking all tokens and sessions for user: {}", userId);

        int revokedTokens = refreshTokenRepo.revokeAllByUserIdWithReason(
                userId, RevokedReason.ADMIN_REVOKED, Instant.now());
        log.info("Revoked {} refresh tokens for user {}", revokedTokens, userId);

        int deactivatedSessions = sessionRepo.deactivateAllUserSessions(userId);
        log.info("Deactivated {} sessions for user {}", deactivatedSessions, userId);

        return revokedTokens + deactivatedSessions;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionStats() {
        Instant now = Instant.now();
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions",          sessionRepo.countByIsActiveTrue());
        stats.put("activeRefreshTokens",     refreshTokenRepo.countActiveTokens(now));
        stats.put("uniqueUsersWithSessions", sessionRepo.countDistinctUsersByIsActiveTrue());
        stats.put("sessionsLast24Hours",     sessionRepo.countByCreatedAtAfter(now.minusSeconds(86_400)));
        return stats;
    }

    private UserSessionResponse toResponse(UserSession s) {
        return UserSessionResponse.builder()
                .id(s.getId())
                .ipAddress(s.getIpAddress())
                .userAgent(s.getUserAgent())
                .isActive(s.getIsActive())
                .createdAt(s.getCreatedAt())
                .expiresAt(s.getExpiresAt())
                .lastAccessedAt(s.getLastAccessedAt())
                .build();
    }
}

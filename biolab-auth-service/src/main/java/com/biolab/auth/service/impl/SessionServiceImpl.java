package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.UserSessionResponse;
import com.biolab.auth.entity.UserSession;
import com.biolab.auth.entity.enums.RevokedReason;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.RefreshTokenRepository;
import com.biolab.auth.repository.UserSessionRepository;
import com.biolab.auth.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of SessionService for managing user sessions and tokens.
 * 
 * @author BioLab Engineering Team
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
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void terminateSession(UUID sessionId) {
        log.info("Terminating session: {}", sessionId);
        UserSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));
        session.setIsActive(false);
        sessionRepo.save(session);
    }

    @Override
    public void terminateAllSessions(UUID userId) {
        log.info("Terminating all sessions for user: {}", userId);
        sessionRepo.deactivateAllUserSessions(userId);
    }

    /**
     * Force logout a user by:
     * 1. Revoking all refresh tokens
     * 2. Deactivating all sessions
     * 
     * The user will be immediately logged out on their next API call
     * because their access token will fail validation (or expire soon)
     * and their refresh token will be invalid.
     */
    @Override
    public int forceLogoutUser(UUID userId) {
        log.warn("🔒 FORCE LOGOUT: Revoking all tokens and sessions for user: {}", userId);
        
        // 1. Revoke all refresh tokens
        int revokedTokens = refreshTokenRepo.revokeAllByUserIdWithReason(userId, RevokedReason.ADMIN_REVOKED.name());
        log.info("Revoked {} refresh tokens for user {}", revokedTokens, userId);
        
        // 2. Deactivate all sessions
        int deactivatedSessions = sessionRepo.deactivateAllUserSessions(userId);
        log.info("Deactivated {} sessions for user {}", deactivatedSessions, userId);
        
        return revokedTokens + deactivatedSessions;
    }

    /**
     * Get platform-wide session statistics for admin dashboard.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionStats() {
        Instant now = Instant.now();
        Map<String, Object> stats = new HashMap<>();
        
        // Count active sessions
        long activeSessions = sessionRepo.countByIsActiveTrue();
        stats.put("activeSessions", activeSessions);
        
        // Count active refresh tokens (non-revoked, non-expired)
        long activeTokens = refreshTokenRepo.countActiveTokens(now);
        stats.put("activeRefreshTokens", activeTokens);
        
        // Count unique users with active sessions
        long uniqueUsers = sessionRepo.countDistinctUsersByIsActiveTrue();
        stats.put("uniqueUsersWithSessions", uniqueUsers);
        
        // Sessions created in last 24 hours
        long recentSessions = sessionRepo.countByCreatedAtAfter(now.minusSeconds(86400));
        stats.put("sessionsLast24Hours", recentSessions);
        
        return stats;
    }

    private UserSessionResponse toResponse(UserSession session) {
        return UserSessionResponse.builder()
                .id(session.getId())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .isActive(session.getIsActive())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .lastAccessedAt(session.getLastAccessedAt())
                .build();
    }
}

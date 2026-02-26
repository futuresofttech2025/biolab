package com.biolab.auth.security;

import com.biolab.auth.repository.LoginAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Detects suspicious login patterns for IP-based anomaly detection.
 *
 * <h3>Detection Rules (Slide 10 — Session Management):</h3>
 * <ul>
 *   <li>Login from new IP address → flag for review</li>
 *   <li>Login from different country → elevated alert</li>
 *   <li>Multiple failed attempts from same IP → potential brute force</li>
 *   <li>Rapid logins across multiple accounts from one IP → credential stuffing</li>
 * </ul>
 *
 * <p>Anomaly scores determine whether to require additional MFA verification
 * or block the login attempt entirely.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginAnomalyDetector {

    private final LoginAuditLogRepository auditLogRepository;

    /**
     * Calculates an anomaly score for a login attempt.
     *
     * @param userId    the user attempting to log in
     * @param ipAddress the client IP address
     * @param userAgent the client User-Agent header
     * @return anomaly score (0 = normal, 1-3 = elevated, 4+ = suspicious)
     */
    public int calculateAnomalyScore(UUID userId, String ipAddress, String userAgent) {
        int score = 0;
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);

        // Check if IP is new for this user (never seen in last 30 days)
        boolean isKnownIp = auditLogRepository.existsByUserIdAndIpAddressAndCreatedAtAfter(
                userId, ipAddress, since);
        if (!isKnownIp) {
            score += 2;
            log.info("Anomaly: New IP {} for user {}", ipAddress, userId);
        }

        // Check for multiple failed attempts from this IP in last hour
        Instant lastHour = Instant.now().minus(1, ChronoUnit.HOURS);
        long failedFromIp = auditLogRepository.countByIpAddressAndStatusAndCreatedAtAfter(
                ipAddress, "FAILURE", lastHour);
        if (failedFromIp >= 3) {
            score += 2;
            log.warn("Anomaly: {} failed logins from IP {} in last hour", failedFromIp, ipAddress);
        }

        // Check for logins to multiple accounts from same IP in last hour
        long distinctUsersFromIp = auditLogRepository.countDistinctUserIdByIpAddressAndCreatedAtAfter(
                ipAddress, lastHour);
        if (distinctUsersFromIp >= 5) {
            score += 3;
            log.warn("Anomaly: {} distinct users from IP {} — possible credential stuffing",
                    distinctUsersFromIp, ipAddress);
        }

        return score;
    }

    /**
     * Determines if a login should be blocked based on anomaly score.
     *
     * @param score the calculated anomaly score
     * @return true if the login should be blocked
     */
    public boolean shouldBlock(int score) {
        return score >= 5;
    }

    /**
     * Determines if additional MFA should be required despite user preference.
     *
     * @param score the calculated anomaly score
     * @return true if forced MFA should be triggered
     */
    public boolean shouldRequireMfa(int score) {
        return score >= 3;
    }
}

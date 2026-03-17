package com.biolab.auth.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.UUID;

/**
 * Redis-backed MFA step-up token.
 *
 * <p>Represents the pending authentication state between a successful
 * credential check and MFA code verification. Created in
 * {@code AuthServiceImpl.login()} when MFA is required, consumed and
 * deleted by {@code AuthServiceImpl.verifyMfa()}.</p>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Login succeeds → {@code MfaPendingToken} saved to Redis (TTL 5 min)</li>
 *   <li>Client posts {@code mfaToken} + {@code code} to {@code /api/auth/mfa/verify}</li>
 *   <li>{@code verifyMfa()} looks up token, validates OTP, deletes token, issues JWT pair</li>
 *   <li>On expiry Redis auto-evicts — next attempt returns 401</li>
 * </ol>
 *
 * @author BioLab Engineering Team
 */
@RedisHash("mfa_pending")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaPendingToken implements Serializable {

    /** The opaque token issued to the client — UUID string, used as Redis key. */
    @Id
    private String token;

    /** The user who passed credential verification and needs MFA. */
    @Indexed
    private UUID userId;

    /** The IP address of the original login request (for audit). */
    private String ipAddress;

    /** The User-Agent of the original login request (for audit). */
    private String userAgent;

    /**
     * TTL in seconds — Spring Data Redis respects this for automatic eviction.
     * 300 seconds = 5 minutes; sufficient for the user to retrieve and enter the code.
     */
    @TimeToLive
    @Builder.Default
    private Long ttlSeconds = 300L;
}

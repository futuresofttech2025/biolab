package com.biolab.auth.repository;

import com.biolab.auth.entity.RefreshToken;
import com.biolab.auth.entity.enums.RevokedReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@code sec_schema.refresh_tokens} — Token Rotation queries.
 *
 * <p>Key operations for the rotation strategy:</p>
 * <ul>
 *   <li>{@link #findByTokenHash} — look up token for validation</li>
 *   <li>{@link #revokeAllByTokenFamily} — invalidate entire family on reuse detection</li>
 *   <li>{@link #revokeAllByUserId} — logout: revoke all user tokens</li>
 *   <li>{@link #revokeAllByUserIdWithReason} — admin revocation with custom reason</li>
 * </ul>
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Find a refresh token by its SHA-256 hash. */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Find all tokens in a given family (for reuse detection audit). */
    List<RefreshToken> findByTokenFamily(UUID tokenFamily);

    /** Find the latest active (non-revoked) token in a family. */
    Optional<RefreshToken> findTopByTokenFamilyAndIsRevokedFalseOrderByGenerationDesc(UUID tokenFamily);

    /** Revoke ALL tokens in a family — triggered on reuse detection. */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.isRevoked = true, t.revokedReason = 'REUSE_DETECTED' " +
           "WHERE t.tokenFamily = :family")
    int revokeAllByTokenFamily(UUID family);

    /** Revoke all active refresh tokens for a user (logout, password change). */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.isRevoked = true, t.revokedReason = 'LOGOUT' " +
           "WHERE t.user.id = :userId AND t.isRevoked = false")
    int revokeAllByUserId(UUID userId);

    /** Revoke all active refresh tokens for a user with custom reason (admin revocation). */
    // Change t.revokedAt to whatever the actual field name is

    @Modifying
    @Query("UPDATE RefreshToken t SET t.isRevoked = true, t.revokedReason = :reason, t.revokedAt = :revokedAt " +
           "WHERE t.user.id = :userId AND t.isRevoked = false")
    int revokeAllByUserIdWithReason(@Param("userId") UUID userId,
                                    @Param("reason") RevokedReason reason,
                                    @Param("revokedAt") Instant revokedAt);

    /** Count active (non-revoked, non-expired) sessions for a user. */
    @Query("SELECT COUNT(t) FROM RefreshToken t WHERE t.user.id = :userId " +
           "AND t.isRevoked = false AND t.expiresAt > :now")
    long countActiveSessions(UUID userId, Instant now);

    /** Count all active tokens across the platform. */
    @Query("SELECT COUNT(t) FROM RefreshToken t WHERE t.isRevoked = false AND t.expiresAt > :now")
    long countActiveTokens(Instant now);

    /** Cleanup expired tokens (scheduled maintenance). */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredTokens(Instant cutoff);

    List<RefreshToken> findByUserIdAndIsRevokedFalse(UUID userId);
}

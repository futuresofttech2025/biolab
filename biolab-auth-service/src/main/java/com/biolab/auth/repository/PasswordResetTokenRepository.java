package com.biolab.auth.repository;

import com.biolab.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link PasswordResetToken}.
 *
 * <p>No findByToken() — the raw token is never stored.
 * Callers must load candidate tokens by userId, then BCrypt-match
 * against {@code token_hash} in-memory.</p>
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Returns all valid (unused, non-expired) tokens for a user.
     * Used during lookup: iterate and BCrypt.matches() against each hash.
     */
    @Query("""
           SELECT t FROM PasswordResetToken t
           WHERE t.user.id = :userId
             AND t.used = false
             AND t.expiresAt > :now
           ORDER BY t.createdAt DESC
           """)
    List<PasswordResetToken> findValidByUserId(@Param("userId") UUID userId,
                                               @Param("now")    Instant now);

    /**
     * Invalidates all outstanding (unused, non-expired) reset tokens for a user.
     * Called when a new reset is requested — prevents token accumulation.
     */
    @Modifying
    @Query("""
           UPDATE PasswordResetToken t
           SET    t.used = true, t.usedAt = :now
           WHERE  t.user.id = :userId
             AND  t.used = false
           """)
    int invalidateAllByUserId(@Param("userId") UUID userId,
                              @Param("now")    Instant now);

    /**
     * Purges expired/used tokens older than the given cutoff.
     * Schedule via {@code @Scheduled} in a maintenance job.
     */
    @Modifying
    @Query("""
           DELETE FROM PasswordResetToken t
           WHERE t.expiresAt < :cutoff
              OR t.used = true
           """)
    int deleteExpiredAndUsed(@Param("cutoff") Instant cutoff);
}
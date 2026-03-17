package com.biolab.auth.repository;

import com.biolab.auth.entity.PasswordResetToken;
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
 * Repository for {@link PasswordResetToken}.
 *
 * <h3>Security design</h3>
 * <p>The raw token is <strong>never stored</strong> — only its SHA-256 hex
 * digest is persisted. The {@link #findByTokenHash(String)} method allows
 * an O(1) indexed lookup of the hash, avoiding the previous {@code findAll()}
 * full-table scan that was a performance and amplification risk.</p>
 *
 * @author BioLab Engineering Team
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Looks up a reset token directly by its SHA-256 hash.
     * The {@code uq_prt_token_hash} unique index makes this O(1).
     * Replaces the previous {@code findAll()} + in-memory scan approach.
     *
     * @param tokenHash hex-encoded SHA-256 digest of the raw token
     * @return the matching token record, if any
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Returns all valid (unused, non-expired) tokens for a user.
     * Used during cleanup and for invalidating outstanding tokens.
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
     * Invalidates all outstanding (unused) reset tokens for a user.
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
     * Called by the scheduled maintenance job in {@code AuthServiceImpl}.
     */
    @Modifying
    @Query("""
           DELETE FROM PasswordResetToken t
           WHERE t.expiresAt < :cutoff
              OR t.used = true
           """)
    int deleteExpiredAndUsed(@Param("cutoff") Instant cutoff);
}

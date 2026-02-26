package com.biolab.auth.repository;

import com.biolab.auth.entity.LoginAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for immutable login audit log entries.
 *
 * <h3>Compliance:</h3>
 * <ul>
 *   <li>HIPAA §164.312(b): Audit controls — every login/logout recorded</li>
 *   <li>FDA 21 CFR Part 11: Tamper-proof, append-only audit trail</li>
 *   <li>7-year retention policy enforced at database level</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 */
@Repository
public interface LoginAuditLogRepository extends JpaRepository<LoginAuditLog, UUID> {

    /** Find audit entries by user ID, ordered by most recent. */
    Page<LoginAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Find audit entries by email (covers pre-authentication lookups). */
    Page<LoginAuditLog> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);

    /** Find audit entries by IP address (for anomaly detection). */
    List<LoginAuditLog> findByIpAddressAndCreatedAtAfterOrderByCreatedAtDesc(
            String ipAddress, Instant since);

    /** Check if a user has previously logged in from a specific IP. */
    boolean existsByUserIdAndIpAddressAndCreatedAtAfter(
            UUID userId, String ipAddress, Instant since);

    /** Count failed login attempts from a specific IP since a given time. */
    @Query("SELECT COUNT(l) FROM LoginAuditLog l " +
           "WHERE l.ipAddress = :ip AND l.status = :status AND l.createdAt > :since")
    long countByIpAddressAndStatusAndCreatedAtAfter(
            @Param("ip") String ipAddress,
            @Param("status") String status,
            @Param("since") Instant since);

    /** Count distinct users who logged in from a specific IP (credential stuffing detection). */
    @Query("SELECT COUNT(DISTINCT l.user.id) FROM LoginAuditLog l WHERE l.ipAddress = :ip AND l.createdAt > :since")
    long countDistinctUserIdByIpAddressAndCreatedAtAfter(@Param("ip") String ipAddress, @Param("since") Instant since);

    /** Find all audit entries within a date range (for compliance reports). */
    Page<LoginAuditLog> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);
}

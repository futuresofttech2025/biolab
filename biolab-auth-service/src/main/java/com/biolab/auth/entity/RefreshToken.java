package com.biolab.auth.entity;

import com.biolab.auth.entity.enums.RevokedReason;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity → {@code sec_schema.refresh_tokens}.
 *
 * <h3>Token Rotation Design:</h3>
 * <p>This entity implements the <b>Refresh Token Rotation</b> pattern
 * recommended by OAuth 2.0 Security Best Current Practice (RFC 9700).</p>
 *
 * <h3>How it works:</h3>
 * <ol>
 *   <li><b>Login:</b> A new token family (UUID) is created with generation=0.</li>
 *   <li><b>Refresh:</b> The current token is revoked (ROTATED), and a new token
 *       is issued in the <em>same family</em> with generation incremented.</li>
 *   <li><b>Reuse Detection:</b> If a revoked token is presented for refresh,
 *       it means either the client or an attacker has a stale token.
 *       The ENTIRE family is revoked immediately (all generations).</li>
 * </ol>
 *
 * <p>This ensures that stolen refresh tokens are detectable: the legitimate
 * client's next refresh attempt will fail, triggering re-authentication.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Entity
@Table(name = "refresh_tokens", schema = "sec_schema",
       uniqueConstraints = @UniqueConstraint(name = "uq_refresh_token_hash", columnNames = "token_hash"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hash of the refresh token value (never store plaintext). */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    /**
     * Token family identifier — all rotated tokens in a login session share
     * the same family UUID. Used for reuse detection: if any revoked token
     * in a family is replayed, ALL tokens in that family are invalidated.
     */
    @Column(name = "token_family", nullable = false)
    private UUID tokenFamily;

    /**
     * Generation counter — incremented on each rotation within the family.
     * Generation 0 = original token from login; 1 = first refresh; etc.
     */
    @Column(name = "generation", nullable = false)
    @Builder.Default
    private Integer generation = 0;

    /** Whether this token has been revoked (used, logged out, or reuse detected). */
    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false;

    /** Reason for revocation — enables audit analysis. */
    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason", length = 100)
    private RevokedReason revokedReason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "issued_at", nullable = false)
    @Builder.Default
    private Instant issuedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;


    /** Marks this token as revoked with the given reason. */
    public void revoke(RevokedReason reason) {
        this.isRevoked = true;
        this.revokedReason = reason;
    }

    /** Returns true if the token has expired. */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /** Returns true if this token can be used for refresh. */
    public boolean isUsable() {
        return !isRevoked && !isExpired();
    }
}

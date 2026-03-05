package com.biolab.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity → {@code sec_schema.password_reset_tokens}
 *
 * <p>Stores a BCrypt-hashed, single-use token for the forgot-password flow.
 * The raw token is generated, emailed to the user, and <strong>never</strong>
 * persisted — only its hash is stored (defence-in-depth: DB breach ≠ token
 * leak).</p>
 *
 * <p>Tokens expire after 15 minutes ({@code PASSWORD_RESET_EXPIRY_MINUTES})
 * and are marked {@code used=true} immediately on consumption to prevent
 * replay attacks.</p>
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(
        name = "password_reset_tokens",
        schema = "sec_schema",
        uniqueConstraints = @UniqueConstraint(name = "uq_prt_token_hash", columnNames = "token_hash")
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Owning user — cascade delete keeps the table clean when users are removed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_prt_user"))
    private User user;

    /**
     * BCrypt hash of the raw 256-bit URL-safe token sent in the email.
     * The raw token is NEVER stored — only this hash.
     */
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    /** Absolute expiry — tokens are valid for {@value #EXPIRY_MINUTES} minutes. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** True once consumed — prevents replay even within the TTL window. */
    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    /** Timestamp of consumption (audit trail). */
    @Column(name = "used_at")
    private Instant usedAt;

    /** IP address of the requester (stored for audit). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ── Constants ──────────────────────────────────────────────────────────

    /** Token TTL in minutes. */
    public static final int EXPIRY_MINUTES = 15;

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Returns true if the token is still within its validity window. */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** Returns true if this token can be used (not expired and not already used). */
    public boolean isValid() {
        return !Boolean.TRUE.equals(used) && !isExpired();
    }
}
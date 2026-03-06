package com.biolab.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity → {@code sec_schema.email_verification_tokens}.
 *
 * <p>One-use token emailed to the user after registration.
 * The raw token is never stored — only its SHA-256 hash.
 * Token expires after 24 hours; {@code used_at} is set when consumed.</p>
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "email_verification_tokens", schema = "sec_schema",
        indexes = @Index(name = "idx_evt_token_hash", columnList = "token_hash"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailVerificationToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hex digest of the raw token. Never store the raw value. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Set once the token has been successfully consumed. */
    @Column(name = "used_at")
    private Instant usedAt;

    /** Returns true if the token has not expired and has not been used. */
    public boolean isValid() {
        return usedAt == null && Instant.now().isBefore(expiresAt);
    }
}
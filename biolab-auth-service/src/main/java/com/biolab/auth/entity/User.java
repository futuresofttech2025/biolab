package com.biolab.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA entity → {@code sec_schema.users}.
 * Core user identity: credentials, lockout state, verification status.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "users", schema = "sec_schema",
       uniqueConstraints = @UniqueConstraint(name = "uq_users_email", columnNames = "email"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "failed_login_count", nullable = false)
    @Builder.Default
    private Integer failedLoginCount = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Returns true if the account is locked AND the lockout has not expired. */
    public boolean isAccountLocked() {
        if (!isLocked) return false;
        if (lockedUntil == null) return true;
        if (Instant.now().isAfter(lockedUntil)) {
            // Auto-unlock after lockout period
            this.isLocked = false;
            this.lockedUntil = null;
            this.failedLoginCount = 0;
            return false;
        }
        return true;
    }

    /** Increments failed login count; locks account if threshold exceeded. */
    public void recordFailedLogin(int maxAttempts, int lockoutMinutes) {
        this.failedLoginCount++;
        if (this.failedLoginCount >= maxAttempts) {
            this.isLocked = true;
            this.lockedUntil = Instant.now().plusSeconds(lockoutMinutes * 60L);
        }
    }

    /** Resets failed count and records successful login timestamp. */
    public void recordSuccessfulLogin() {
        this.failedLoginCount = 0;
        this.isLocked = false;
        this.lockedUntil = null;
        this.lastLoginAt = Instant.now();
    }
}

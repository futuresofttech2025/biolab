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

    @Column(name = "phone", length = 30)
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

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Lifecycle callback — ensures updated_at is always set on insert and update.
     * This prevents NOT NULL constraint violations even if the builder or service
     * code forgets to set it explicitly.
     */
    @PrePersist
    protected void onPrePersist() {
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
        if (this.passwordChangedAt == null) {
            this.passwordChangedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

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

    /**
     * Clears any active lockout state without recording a login.
     * Called after a successful password reset so the user is not
     * still locked out when they try to log in with their new password.
     */
    public void clearLockout() {
        this.isLocked = false;
        this.lockedUntil = null;
        this.failedLoginCount = 0;
    }
}
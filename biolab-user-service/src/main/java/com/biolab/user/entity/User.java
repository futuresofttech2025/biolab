package com.biolab.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA entity → {@code sec_schema.users}.
 * Used by User Service for profile management (no password operations).
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "users", schema = "sec_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** Password hash — read-only in User Service (Auth Service manages passwords). */
    @Column(name = "password_hash", nullable = false, length = 255, insertable = false, updatable = false)
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

    @Column(name = "failed_login_count", nullable = false, insertable = false, updatable = false)
    private Integer failedLoginCount;

    @Column(name = "locked_until", insertable = false, updatable = false)
    private Instant lockedUntil;

    @Column(name = "last_login_at", insertable = false, updatable = false)
    private Instant lastLoginAt;

    @Column(name = "password_changed_at", insertable = false, updatable = false)
    private Instant passwordChangedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

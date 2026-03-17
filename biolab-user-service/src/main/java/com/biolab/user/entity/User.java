package com.biolab.user.entity;

import com.biolab.common.encryption.DeterministicStringConverter;
import com.biolab.common.encryption.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA entity → {@code sec_schema.users}.
 * Used by User Service for profile management (no password operations).
 *
 * <h3>Sprint 2 — GAP-12 / GAP-13: PII encryption applied</h3>
 * <p>Mirrors the encryption annotations on the Auth Service {@code User} entity.
 * Both services share the same DB table; both must apply identical converters
 * so JPA reads/writes are consistent.</p>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@Entity
@Table(name = "users", schema = "sec_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    /** HMAC-SHA256 lookup hash — not displayable. Use emailDisplay. */
    @Convert(converter = DeterministicStringConverter.class)
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /** AES-256-GCM encrypted email for display. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "email_display", length = 512)
    private String emailDisplay;

    @Column(name = "password_hash", nullable = false, length = 255,
            insertable = false, updatable = false)
    private String passwordHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "first_name", nullable = false, length = 512)
    private String firstName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "last_name", nullable = false, length = 512)
    private String lastName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone", length = 512)
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

    @Column(name = "failed_login_count", nullable = false,
            insertable = false, updatable = false)
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
package com.biolab.auth.entity;

import com.biolab.auth.entity.enums.MfaType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA entity → {@code sec_schema.mfa_settings}.
 * MFA config per user: TOTP (Google Authenticator) or Email OTP.
 *
 * <h3>FIX-14: emailOtpExpiresAt</h3>
 * <p>The {@code email_otp_expires_at} column enforces the 10-minute
 * expiry window promised in the MFA OTP email. Previously the code was
 * stored indefinitely until consumed, allowing stale OTPs to be accepted.
 * The column is {@code NULL} for TOTP records.</p>
 *
 * <h3>DB migration required</h3>
 * <pre>{@code
 *   ALTER TABLE sec_schema.mfa_settings
 *     ADD COLUMN IF NOT EXISTS email_otp_expires_at TIMESTAMPTZ;
 * }</pre>
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "mfa_settings", schema = "sec_schema",
       uniqueConstraints = @UniqueConstraint(name = "uq_user_mfa_type",
               columnNames = {"user_id", "mfa_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MfaSettings extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_type", nullable = false, length = 20)
    private MfaType mfaType;

    /** TOTP: Base32 secret. EMAIL: 6-digit OTP (cleared after use). */
    @Column(name = "secret_key", length = 255)
    private String secretKey;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    @Column(name = "backup_codes", columnDefinition = "text[]")
    private String[] backupCodes;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    /**
     * FIX-14: expiry for EMAIL OTP codes — {@code NULL} for TOTP records.
     * Set to {@code now + 10 min} when an email OTP is issued; checked in
     * {@code MfaServiceImpl.validateEmailOtp()} before accepting the code.
     */
    @Column(name = "email_otp_expires_at")
    private Instant emailOtpExpiresAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

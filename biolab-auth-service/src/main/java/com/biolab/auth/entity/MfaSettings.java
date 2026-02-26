package com.biolab.auth.entity;

import com.biolab.auth.entity.enums.MfaType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA entity → {@code sec_schema.mfa_settings}.
 * MFA config per user: TOTP (Google Authenticator) or Email OTP.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "mfa_settings", schema = "sec_schema",
       uniqueConstraints = @UniqueConstraint(name = "uq_user_mfa_type", columnNames = {"user_id", "mfa_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MfaSettings extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_type", nullable = false, length = 20)
    private MfaType mfaType;

    @Column(name = "secret_key", length = 255)
    private String secretKey;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    @Column(name = "backup_codes", columnDefinition = "text[]")
    private String[] backupCodes;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

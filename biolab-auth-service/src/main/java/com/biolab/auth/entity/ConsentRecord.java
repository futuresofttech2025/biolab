package com.biolab.auth.entity;

import com.biolab.auth.entity.enums.ConsentType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA entity → {@code sec_schema.consent_records}.
 * GDPR/HIPAA consent tracking with grant/revoke timestamps.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "consent_records", schema = "sec_schema",
       uniqueConstraints = @UniqueConstraint(name = "uq_user_consent", columnNames = {"user_id", "consent_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsentRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 20)
    private ConsentType consentType;

    @Column(name = "granted_at", nullable = false)
    @Builder.Default
    private Instant grantedAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "version", nullable = false, length = 20)
    @Builder.Default
    private String version = "1.0";
}

package com.biolab.auth.entity;

import com.biolab.auth.entity.enums.LoginAction;
import com.biolab.auth.entity.enums.LoginStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity → {@code sec_schema.login_audit_log}.
 * Immutable audit record for every authentication event (HIPAA).
 * Includes TOKEN_ROTATION and REUSE_DETECTED actions.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "login_audit_log", schema = "sec_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginAuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private LoginAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoginStatus status;

    @Column(name = "mfa_used", nullable = false)
    @Builder.Default
    private Boolean mfaUsed = false;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;
}

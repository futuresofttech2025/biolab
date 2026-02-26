package com.biolab.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity → {@code sec_schema.user_roles}.
 * M:N junction: users ↔ roles with assignment audit trail.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "user_roles", schema = "sec_schema",
       uniqueConstraints = @UniqueConstraint(name = "uq_user_role", columnNames = {"user_id", "role_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;
}

package com.biolab.auth.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity → {@code sec_schema.role_permissions}.
 * M:N junction: roles ↔ permissions.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "role_permissions", schema = "sec_schema",
       uniqueConstraints = @UniqueConstraint(name = "uq_role_permission", columnNames = {"role_id", "permission_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;
}

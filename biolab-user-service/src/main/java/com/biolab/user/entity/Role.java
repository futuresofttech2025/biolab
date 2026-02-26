package com.biolab.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA entity → {@code sec_schema.roles}.
 * RBAC role definitions: SUPER_ADMIN, ADMIN, SUPPLIER, BUYER + custom.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "roles", schema = "sec_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_system_role", nullable = false)
    @Builder.Default
    private Boolean isSystemRole = false;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

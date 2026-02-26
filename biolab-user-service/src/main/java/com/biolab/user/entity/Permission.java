package com.biolab.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA entity → {@code sec_schema.permissions}.
 * Granular action-level permissions grouped by module.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "permissions", schema = "sec_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

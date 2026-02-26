package com.biolab.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity → {@code app_schema.user_organizations}.
 * User-Organization membership with role and primary flag.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "user_organizations", schema = "app_schema",
       uniqueConstraints = @UniqueConstraint(name = "uq_user_org", columnNames = {"user_id", "org_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserOrganization extends BaseEntity {

    /** User UUID — cross-schema reference to sec_schema.users. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "role_in_org", nullable = false, length = 50)
    @Builder.Default
    private String roleInOrg = "MEMBER";

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private Instant joinedAt = Instant.now();
}

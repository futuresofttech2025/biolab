package com.biolab.user.entity;

import com.biolab.user.entity.enums.OrganizationType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA entity → {@code app_schema.organizations}.
 * Buyer and Supplier organizations on the BioLabs platform.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "organizations", schema = "app_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private OrganizationType type;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

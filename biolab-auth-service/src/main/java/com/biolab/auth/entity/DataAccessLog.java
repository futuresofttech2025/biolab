package com.biolab.auth.entity;

import com.biolab.auth.entity.enums.DataAccessAction;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * JPA entity → {@code sec_schema.data_access_log}.
 * PHI/PII access audit trail — every view/download logged (HIPAA).
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "data_access_log", schema = "sec_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DataAccessLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private DataAccessAction action;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
}

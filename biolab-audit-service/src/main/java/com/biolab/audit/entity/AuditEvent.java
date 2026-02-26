package com.biolab.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "audit_events", schema = "app_schema")
public class AuditEvent {
    @Id @GeneratedValue private UUID id;
    @Setter
    @Column(name = "user_id") private UUID userId;
    @Setter
    @Column(nullable = false, length = 100) private String action;
    @Setter
    @Column(name = "entity_type", nullable = false, length = 50) private String entityType;
    @Setter
    @Column(name = "entity_id") private UUID entityId;
    @Setter
    @Column(columnDefinition = "jsonb") private String details;
    @Setter
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "created_at") private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

}

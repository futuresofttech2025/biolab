package com.biolab.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "platform_settings", schema = "app_schema")
public class PlatformSetting {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false, unique = true, length = 100) private String key;
    @Column(nullable = false, columnDefinition = "TEXT") private String value;
    @Column(length = 50) private String category = "GENERAL";
    @Column(name = "updated_by") private UUID updatedBy;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

}

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
@Table(name = "policy_documents", schema = "app_schema")
public class PolicyDocument {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, length = 20) private String version;
    @Column(length = 20) private String status = "CURRENT";
    @Column(name = "content_url", length = 512) private String contentUrl;
    @Column(name = "updated_at") private Instant updatedAt;
    @Column(name = "created_at") private Instant createdAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }

}

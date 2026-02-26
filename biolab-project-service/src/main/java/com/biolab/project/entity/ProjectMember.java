package com.biolab.project.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_members", schema = "app_schema")
public class ProjectMember {
    @Id @GeneratedValue private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    private String role = "MEMBER";
    @Column(name = "added_at") private Instant addedAt;

    @PrePersist void onCreate() { addedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID id) { this.projectId = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID id) { this.userId = id; }
    public String getRole() { return role; }
    public void setRole(String r) { this.role = r; }
    public Instant getAddedAt() { return addedAt; }
}

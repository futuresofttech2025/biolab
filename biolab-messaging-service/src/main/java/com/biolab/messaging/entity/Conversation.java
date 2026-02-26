package com.biolab.messaging.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "conversations", schema = "app_schema")
public class Conversation {
    @Id @GeneratedValue private UUID id;
    @Column(name = "project_id") private UUID projectId;
    private String title;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID id) { this.projectId = id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant u) { this.updatedAt = u; }
}

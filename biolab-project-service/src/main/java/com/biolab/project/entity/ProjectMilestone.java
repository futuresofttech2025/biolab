package com.biolab.project.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "project_milestones", schema = "app_schema")
public class ProjectMilestone {
    @Id @GeneratedValue private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(nullable = false) private String title;
    private String description;
    @Column(name = "milestone_date") private LocalDate milestoneDate;
    @Column(name = "is_completed") private Boolean isCompleted = false;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "sort_order") private Integer sortOrder = 0;
    @Column(name = "created_at") private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID id) { this.projectId = id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public LocalDate getMilestoneDate() { return milestoneDate; }
    public void setMilestoneDate(LocalDate d) { this.milestoneDate = d; }
    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean c) { this.isCompleted = c; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant c) { this.completedAt = c; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer s) { this.sortOrder = s; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.biolab.catalog.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_categories", schema = "app_schema")
public class ServiceCategory {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false, unique = true) private String name;
    @Column(nullable = false, unique = true) private String slug;
    private String description;
    private String icon;
    @Column(name = "sort_order") private Integer sortOrder = 0;
    @Column(name = "is_active") private Boolean isActive = true;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getIcon() { return icon; }
    public void setIcon(String i) { this.icon = i; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer s) { this.sortOrder = s; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean a) { this.isActive = a; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

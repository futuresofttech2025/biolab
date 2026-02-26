package com.biolab.catalog.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_requests", schema = "app_schema")
public class ServiceRequest {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false) private Service service;
    @Column(name = "buyer_id", nullable = false) private UUID buyerId;
    @Column(name = "buyer_org_id") private UUID buyerOrgId;
    @Column(name = "sample_type") private String sampleType;
    private String timeline = "STANDARD";
    private String requirements;
    private String priority = "MEDIUM";
    private String status = "PENDING";
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Service getService() { return service; }
    public void setService(Service s) { this.service = s; }
    public UUID getBuyerId() { return buyerId; }
    public void setBuyerId(UUID id) { this.buyerId = id; }
    public UUID getBuyerOrgId() { return buyerOrgId; }
    public void setBuyerOrgId(UUID id) { this.buyerOrgId = id; }
    public String getSampleType() { return sampleType; }
    public void setSampleType(String s) { this.sampleType = s; }
    public String getTimeline() { return timeline; }
    public void setTimeline(String t) { this.timeline = t; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String r) { this.requirements = r; }
    public String getPriority() { return priority; }
    public void setPriority(String p) { this.priority = p; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

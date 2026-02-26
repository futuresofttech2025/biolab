package com.biolab.project.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "projects", schema = "app_schema")
public class Project {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private String title;
    @Column(name = "service_request_id") private UUID serviceRequestId;
    @Column(name = "buyer_org_id", nullable = false) private UUID buyerOrgId;
    @Column(name = "supplier_org_id", nullable = false) private UUID supplierOrgId;
    private String status = "PENDING";
    @Column(name = "progress_pct") private Integer progressPct = 0;
    private BigDecimal budget;
    @Column(name = "start_date") private LocalDate startDate;
    private LocalDate deadline;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }

    // All getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public UUID getServiceRequestId() { return serviceRequestId; }
    public void setServiceRequestId(UUID id) { this.serviceRequestId = id; }
    public UUID getBuyerOrgId() { return buyerOrgId; }
    public void setBuyerOrgId(UUID id) { this.buyerOrgId = id; }
    public UUID getSupplierOrgId() { return supplierOrgId; }
    public void setSupplierOrgId(UUID id) { this.supplierOrgId = id; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public Integer getProgressPct() { return progressPct; }
    public void setProgressPct(Integer p) { this.progressPct = p; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal b) { this.budget = b; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate d) { this.startDate = d; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate d) { this.deadline = d; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant c) { this.completedAt = c; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

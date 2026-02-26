package com.biolab.catalog.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "services", schema = "app_schema")
public class Service {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String slug;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false) private ServiceCategory category;
    @Column(name = "supplier_org_id", nullable = false) private UUID supplierOrgId;
    private String description;
    private String methodology;
    @Column(name = "price_from") private BigDecimal priceFrom;
    private String turnaround;
    private BigDecimal rating = BigDecimal.ZERO;
    @Column(name = "review_count") private Integer reviewCount = 0;
    @Column(name = "is_active") private Boolean isActive = true;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getSlug() { return slug; }
    public void setSlug(String s) { this.slug = s; }
    public ServiceCategory getCategory() { return category; }
    public void setCategory(ServiceCategory c) { this.category = c; }
    public UUID getSupplierOrgId() { return supplierOrgId; }
    public void setSupplierOrgId(UUID id) { this.supplierOrgId = id; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getMethodology() { return methodology; }
    public void setMethodology(String m) { this.methodology = m; }
    public BigDecimal getPriceFrom() { return priceFrom; }
    public void setPriceFrom(BigDecimal p) { this.priceFrom = p; }
    public String getTurnaround() { return turnaround; }
    public void setTurnaround(String t) { this.turnaround = t; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal r) { this.rating = r; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer c) { this.reviewCount = c; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean a) { this.isActive = a; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

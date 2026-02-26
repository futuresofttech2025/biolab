package com.biolab.invoice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "invoices", schema = "app_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {
    @Id @GeneratedValue private UUID id;
    @Column(name = "invoice_number", unique = true, nullable = false) private String invoiceNumber;
    @Column(name = "project_id") private UUID projectId;
    @Column(name = "supplier_org_id", nullable = false) private UUID supplierOrgId;
    @Column(name = "buyer_org_id", nullable = false) private UUID buyerOrgId;
    @Builder.Default private String status = "DRAFT";
    @Builder.Default private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "tax_rate") @Builder.Default private BigDecimal taxRate = BigDecimal.ZERO;
    @Column(name = "tax_amount") @Builder.Default private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(name = "tax_label", length = 50) private String taxLabel;
    @Builder.Default private BigDecimal total = BigDecimal.ZERO;
    @Column(name = "issue_date") private LocalDate issueDate;
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(name = "paid_date") private LocalDate paidDate;
    private String notes;
    @Column(name = "created_at", updatable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder") @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); if (issueDate == null) issueDate = LocalDate.now(); }

    public void recalculate() {
        this.subtotal = items.stream().map(InvoiceItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.taxAmount = subtotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.total = subtotal.add(taxAmount);
    }
}

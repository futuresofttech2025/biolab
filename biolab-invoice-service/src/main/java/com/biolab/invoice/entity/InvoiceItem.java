package com.biolab.invoice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity @Table(name = "invoice_items", schema = "app_schema")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false) private Invoice invoice;
    @Column(nullable = false) private String description;
    private Integer quantity = 1;
    @Column(name = "unit_price", nullable = false) private BigDecimal unitPrice;
    @Column(nullable = false) private BigDecimal amount;
    @Column(name = "sort_order") private Integer sortOrder = 0;

    public void setId(UUID id) { this.id = id; }

    public void setInvoice(Invoice i) { this.invoice = i; }

    public void setDescription(String d) { this.description = d; }

    public void setQuantity(Integer q) { this.quantity = q; }

    public void setUnitPrice(BigDecimal p) { this.unitPrice = p; }

    public void setAmount(BigDecimal a) { this.amount = a; }

    public void setSortOrder(Integer s) { this.sortOrder = s; }

    @PrePersist @PreUpdate void calc() { this.amount = unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}

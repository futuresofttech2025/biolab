package com.biolab.invoice.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

public record InvoiceDto(
    UUID id,
    String invoiceNumber,
    UUID projectId,
    UUID supplierOrgId,
    UUID buyerOrgId,
    String status,
    BigDecimal subtotal,
    BigDecimal taxAmount,
    BigDecimal total,
    LocalDate issueDate,
    LocalDate dueDate,
    LocalDate paidDate,
    String notes,
    List<InvoiceItemDto> items,
    Instant createdAt,
    BigDecimal taxRate,
    String taxLabel
) {
    /** Backward-compatible constructor (no taxRate/taxLabel). */
    public InvoiceDto(UUID id, String invoiceNumber, UUID projectId,
                      UUID supplierOrgId, UUID buyerOrgId, String status,
                      BigDecimal subtotal, BigDecimal taxAmount, BigDecimal total,
                      LocalDate issueDate, LocalDate dueDate, LocalDate paidDate,
                      String notes, List<InvoiceItemDto> items, Instant createdAt) {
        this(id, invoiceNumber, projectId, supplierOrgId, buyerOrgId, status,
             subtotal, taxAmount, total, issueDate, dueDate, paidDate,
             notes, items, createdAt, BigDecimal.ZERO, null);
    }
}

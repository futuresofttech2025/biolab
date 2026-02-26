package com.biolab.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating an invoice.
 *
 * <h3>Taxation (v4.3):</h3>
 * <p>The {@code taxRate} is sourced from Admin Settings → Taxation tab
 * (PlatformSetting key: {@code taxation.default_tax_rate}).
 * Frontend fetches the platform tax rate and includes it here.
 * If null, defaults to 0% (no tax).</p>
 *
 * @param buyerOrgId  buyer organization UUID
 * @param projectId   project UUID (optional)
 * @param items       line items (description, qty, unitPrice)
 * @param dueDate     payment due date (defaults to +30 days)
 * @param taxRate     tax percentage from platform settings (e.g. 18.00 for 18%)
 * @param taxLabel    tax name from platform settings (e.g. "GST", "VAT")
 * @param notes       optional notes
 */
public record CreateInvoiceRequest(
    UUID buyerOrgId,
    UUID projectId,
    List<CreateInvoiceItemRequest> items,
    LocalDate dueDate,
    BigDecimal taxRate,
    String taxLabel,
    String notes
) {}

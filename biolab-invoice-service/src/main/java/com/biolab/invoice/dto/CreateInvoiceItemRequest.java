package com.biolab.invoice.dto;

import java.math.BigDecimal;

public record CreateInvoiceItemRequest(
    String description,
    Integer quantity,
    BigDecimal unitPrice
) {}

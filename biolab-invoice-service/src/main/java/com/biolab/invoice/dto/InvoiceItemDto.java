package com.biolab.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemDto(
    UUID id,
    String description,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal amount
) {}

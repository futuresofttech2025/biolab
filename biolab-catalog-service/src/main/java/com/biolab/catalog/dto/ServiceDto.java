package com.biolab.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "ServiceDto response")
public record ServiceDto(UUID id, String name, String slug, String categoryName, String categorySlug,
                          UUID supplierOrgId, String description, String methodology,
                          BigDecimal priceFrom, String turnaround, BigDecimal rating,
                          Integer reviewCount, Boolean isActive) {}

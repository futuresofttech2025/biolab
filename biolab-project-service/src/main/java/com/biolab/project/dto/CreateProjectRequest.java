package com.biolab.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request payload for creating a new project")
public record CreateProjectRequest(
    @NotBlank(message = "Project title is required")
    @Size(min = 3, max = 255, message = "Title must be 3-255 characters")
    @Schema(description = "Project title", example = "Enzyme Kinetics — Batch 2024-A")
    String title,

    @NotNull(message = "Buyer organization ID is required")
    @Schema(description = "Buyer organization UUID")
    UUID buyerOrgId,

    @NotNull(message = "Supplier organization ID is required")
    @Schema(description = "Supplier organization UUID")
    UUID supplierOrgId,

    @Schema(description = "Associated service request ID (optional)")
    UUID serviceRequestId,

    @DecimalMin(value = "0.00", message = "Budget must be non-negative")
    @Schema(description = "Project budget in USD", example = "8500.00")
    BigDecimal budget,

    @Schema(description = "Project start date")
    LocalDate startDate,

    @Schema(description = "Project deadline")
    LocalDate deadline
) {}

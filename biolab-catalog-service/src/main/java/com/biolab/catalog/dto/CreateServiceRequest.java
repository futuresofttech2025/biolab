package com.biolab.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request payload for creating a new service")
public record CreateServiceRequest(
    @NotBlank(message = "Service name is required")
    @Size(min = 3, max = 255, message = "Name must be 3-255 characters")
    @Schema(description = "Service display name", example = "Enzyme Kinetics Analysis")
    String name,

    @NotNull(message = "Category ID is required")
    @Schema(description = "Category UUID")
    UUID categoryId,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Service description")
    String description,

    @Size(max = 5000, message = "Methodology must not exceed 5000 characters")
    @Schema(description = "Detailed methodology")
    String methodology,

    @DecimalMin(value = "0.01", message = "Price must be positive")
    @Schema(description = "Starting price in USD", example = "2800.00")
    BigDecimal priceFrom,

    @Size(max = 50, message = "Turnaround must not exceed 50 characters")
    @Schema(description = "Estimated turnaround time", example = "3-7 days")
    String turnaround
) {}

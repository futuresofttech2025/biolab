package com.biolab.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Schema(description = "Request payload for creating a project milestone")
public record CreateMilestoneRequest(
    @NotBlank(message = "Milestone title is required")
    @Size(min = 2, max = 255, message = "Title must be 2-255 characters")
    @Schema(description = "Milestone title", example = "Protocol Approved")
    String title,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Milestone description")
    String description,

    @Schema(description = "Target completion date")
    LocalDate milestoneDate,

    @Schema(description = "Sort order", example = "1")
    Integer sortOrder
) {}

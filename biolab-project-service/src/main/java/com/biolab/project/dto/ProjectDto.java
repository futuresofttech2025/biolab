package com.biolab.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Schema(description = "ProjectDto response")
public record ProjectDto(UUID id, String title, UUID buyerOrgId, UUID supplierOrgId,
                          String status, Integer progressPct, BigDecimal budget,
                          LocalDate startDate, LocalDate deadline, Instant createdAt,
                          List<MilestoneDto> milestones) {}

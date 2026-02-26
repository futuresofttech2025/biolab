package com.biolab.project.dto;

import java.time.*;
import java.util.UUID;

public record MilestoneDto(UUID id, String title, String description, LocalDate milestoneDate,
                            Boolean isCompleted, Integer sortOrder) {}

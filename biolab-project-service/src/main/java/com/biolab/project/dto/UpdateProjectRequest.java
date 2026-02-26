package com.biolab.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProjectRequest(String title, String status, Integer progressPct,
                                    BigDecimal budget, LocalDate deadline) {}

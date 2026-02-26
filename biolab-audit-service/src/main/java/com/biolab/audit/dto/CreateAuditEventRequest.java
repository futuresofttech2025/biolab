package com.biolab.audit.dto;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
public record CreateAuditEventRequest(UUID userId, @NotBlank String action,
    @NotBlank String entityType, UUID entityId, String details, String ipAddress) {}

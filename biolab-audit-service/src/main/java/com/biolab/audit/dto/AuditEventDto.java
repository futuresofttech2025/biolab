package com.biolab.audit.dto;
import java.time.Instant;
import java.util.UUID;
public record AuditEventDto(UUID id, UUID userId, String action, String entityType,
    UUID entityId, String details, String ipAddress, Instant createdAt) {}

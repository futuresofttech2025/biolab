package com.biolab.audit.dto;
import java.time.Instant;
import java.util.UUID;
public record PolicyDocumentDto(UUID id, String name, String version, String status,
    String contentUrl, Instant updatedAt) {}

package com.biolab.document.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentDto(UUID id, UUID projectId, UUID uploadedBy, String fileName,
                           String fileType, Long fileSize, String mimeType,
                           Integer version, Instant createdAt) {}

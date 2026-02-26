package com.biolab.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(UUID id, UUID conversationId, UUID senderId, String content,
                          UUID attachmentId, Boolean isRead, Instant createdAt) {}

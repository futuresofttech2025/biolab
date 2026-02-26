package com.biolab.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationDto(UUID id, UUID projectId, String title, Instant updatedAt,
                               String lastMessage, String lastMessageTime, long unreadCount) {}

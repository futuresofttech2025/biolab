package com.biolab.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(UUID id, String type, String title, String message,
    String link, boolean isRead, Instant createdAt) {}

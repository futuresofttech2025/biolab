package com.biolab.notification.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateNotificationRequest(
    UUID userId,
    @NotBlank String type,
    @NotBlank String title,
    String message,
    String link) {}

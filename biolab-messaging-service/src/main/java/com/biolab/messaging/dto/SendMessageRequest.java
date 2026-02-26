package com.biolab.messaging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;

@Schema(description = "Request payload for sending a message")
public record SendMessageRequest(
    @NotBlank(message = "Message content is required")
    @Size(max = 10000, message = "Message must not exceed 10000 characters")
    @Schema(description = "Message text content")
    String content,

    @Schema(description = "Optional document attachment ID")
    UUID attachmentId
) {}

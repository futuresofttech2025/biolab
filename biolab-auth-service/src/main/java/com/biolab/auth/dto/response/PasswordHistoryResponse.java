package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Password history entry (hash not exposed). */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordHistoryResponse {
    private UUID id;
    private Instant createdAt;
}

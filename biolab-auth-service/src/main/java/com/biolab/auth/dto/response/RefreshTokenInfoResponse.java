package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Refresh token info (for admin view — no raw token exposed). */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshTokenInfoResponse {
    private UUID id;
    private UUID tokenFamily;
    private Integer generation;
    private Boolean isRevoked;
    private String revokedReason;
    private String ipAddress;
    private Instant issuedAt;
    private Instant expiresAt;
}

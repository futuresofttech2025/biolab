package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Blacklisted token response. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class JwtTokenBlacklistResponse {
    private UUID id;
    private String jti;
    private String tokenType;
    private Instant expiresAt;
    private Instant blacklistedAt;
    private String reason;
}

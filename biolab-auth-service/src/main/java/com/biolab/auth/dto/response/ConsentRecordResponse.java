package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Consent record response. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsentRecordResponse {
    private UUID id;
    private String consentType;
    private Instant grantedAt;
    private Instant revokedAt;
    private String ipAddress;
    private String version;
}

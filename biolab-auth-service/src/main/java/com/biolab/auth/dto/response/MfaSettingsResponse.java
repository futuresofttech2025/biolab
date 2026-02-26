package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** MFA settings for a user. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MfaSettingsResponse {
    private UUID id;
    private String mfaType;
    private Boolean isEnabled;
    private Instant verifiedAt;
}

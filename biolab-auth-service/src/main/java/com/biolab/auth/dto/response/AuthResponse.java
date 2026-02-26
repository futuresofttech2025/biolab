package com.biolab.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Authentication response — JWT tokens or MFA challenge.
 * Includes token rotation metadata (family, generation).
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default private String tokenType = "Bearer";
    private long expiresIn;
    private Boolean mfaRequired;
    private String mfaToken;
    /** Token family UUID — same across rotations within a login session. */
    private String tokenFamily;
    /** Rotation generation — 0=login, 1=first refresh, etc. */
    private Integer tokenGeneration;
}

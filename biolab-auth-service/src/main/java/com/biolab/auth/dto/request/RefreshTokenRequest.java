package com.biolab.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** Refresh token request — triggers token rotation. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}

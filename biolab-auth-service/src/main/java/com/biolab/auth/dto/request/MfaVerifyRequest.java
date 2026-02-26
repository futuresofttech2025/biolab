package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** MFA verification — temp token + 6-digit code. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MfaVerifyRequest {
    @NotBlank private String mfaToken;
    @NotBlank @Size(min = 6, max = 6) private String code;
}

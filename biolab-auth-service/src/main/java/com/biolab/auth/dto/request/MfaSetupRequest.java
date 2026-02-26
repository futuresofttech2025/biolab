package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** MFA setup — choose TOTP or EMAIL. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MfaSetupRequest {
    @NotBlank @Pattern(regexp = "^(TOTP|EMAIL)$", message = "Must be TOTP or EMAIL")
    private String mfaType;
}

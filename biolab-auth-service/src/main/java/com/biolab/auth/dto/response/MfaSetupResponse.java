package com.biolab.auth.dto.response;

import lombok.*;
import java.util.List;

/** MFA setup response — TOTP secret + QR URL + backup codes. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MfaSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private List<String> backupCodes;
}

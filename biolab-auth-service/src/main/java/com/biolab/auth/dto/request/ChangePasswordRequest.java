package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Change-password request — requires the current password for re-authentication
 * before the new one is accepted (prevents account takeover via unlocked session).
 *
 * <p>Both fields carry the same complexity {@code @Pattern} to ensure the new
 * password meets the same strength requirements enforced at registration and
 * password-reset time.</p>
 *
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(
        regexp  = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^])[A-Za-z\\d@$!%*?&#^]{8,}$",
        message = "Must contain uppercase, lowercase, digit, and special character"
    )
    private String newPassword;
}

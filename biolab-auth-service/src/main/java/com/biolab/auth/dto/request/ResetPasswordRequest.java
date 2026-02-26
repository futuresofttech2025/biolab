package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Reset password — token + new password. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ResetPasswordRequest {
    @NotBlank private String token;
    @NotBlank @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^])[A-Za-z\\d@$!%*?&#^]{8,}$")
    private String newPassword;
}

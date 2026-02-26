package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Change password request — requires current password verification. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangePasswordRequest {
    @NotBlank private String currentPassword;
    @NotBlank @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^])[A-Za-z\\d@$!%*?&#^]{8,}$",
             message = "Must contain uppercase, lowercase, digit, and special character")
    private String newPassword;
}

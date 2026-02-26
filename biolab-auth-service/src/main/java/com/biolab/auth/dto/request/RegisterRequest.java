package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Registration request — email, password (strength-validated), name. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
    @NotBlank(message = "Email is required") @Email @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required") @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^])[A-Za-z\\d@$!%*?&#^]{8,}$",
             message = "Must contain uppercase, lowercase, digit, and special character")
    private String password;

    @NotBlank @Size(min = 1, max = 100) private String firstName;
    @NotBlank @Size(min = 1, max = 100) private String lastName;
    @Size(max = 20) private String phone;
    private String organizationId;
    private String role;
}

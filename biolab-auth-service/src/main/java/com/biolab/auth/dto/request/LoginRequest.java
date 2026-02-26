package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Login request — email + password. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginRequest {
    @NotBlank @Email private String email;
    @NotBlank private String password;
}

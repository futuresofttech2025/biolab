package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Forgot password — triggers reset email. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ForgotPasswordRequest {
    @NotBlank @Email private String email;
}

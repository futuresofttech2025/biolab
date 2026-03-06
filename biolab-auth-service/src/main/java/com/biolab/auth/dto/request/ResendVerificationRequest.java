package com.biolab.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** Request to resend email verification link. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ResendVerificationRequest {
    @NotBlank @Email
    private String email;
}
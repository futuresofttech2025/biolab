package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Admin: create a user account directly. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserCreateRequest {
    @NotBlank @Email @Size(max = 255) private String email;
    @NotBlank @Size(min = 8, max = 128) private String password;
    @NotBlank @Size(max = 100) private String firstName;
    @NotBlank @Size(max = 100) private String lastName;
    @Size(max = 20) private String phone;
    private Boolean isActive;
    private Boolean isEmailVerified;
}

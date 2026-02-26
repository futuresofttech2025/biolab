package com.biolab.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/** Update user profile. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserUpdateRequest {
    @Size(max = 100) private String firstName;
    @Size(max = 100) private String lastName;
    @Size(max = 20) private String phone;
    @Size(max = 512) private String avatarUrl;
    private Boolean isActive;
    private Boolean isEmailVerified;
}

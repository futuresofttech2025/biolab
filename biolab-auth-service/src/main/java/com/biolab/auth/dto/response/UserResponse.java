package com.biolab.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** User profile response. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private Boolean isLocked;
    private List<String> roles;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}

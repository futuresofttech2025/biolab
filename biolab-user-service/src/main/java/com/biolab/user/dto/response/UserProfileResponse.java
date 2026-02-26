package com.biolab.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User profile response — includes roles and organization info.
 * Password hash is never included.
 *
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private Boolean isLocked;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;

    /** User's assigned role names (e.g., ["BUYER", "ADMIN"]). */
    private List<String> roles;

    /** User's organization memberships. */
    private List<UserOrganizationResponse> organizations;
}

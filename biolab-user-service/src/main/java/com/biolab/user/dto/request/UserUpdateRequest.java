package com.biolab.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * User profile update request — partial update, only non-null fields are applied.
 *
 * <p>Note: Email and password changes are handled by the Auth Service.
 * This DTO covers profile-level updates only.</p>
 *
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserUpdateRequest {

    @Size(min = 1, max = 100, message = "First name must be 1-100 characters")
    private String firstName;

    @Size(min = 1, max = 100, message = "Last name must be 1-100 characters")
    private String lastName;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 512, message = "Avatar URL must not exceed 512 characters")
    private String avatarUrl;
}

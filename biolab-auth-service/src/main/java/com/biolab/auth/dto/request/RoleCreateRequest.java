package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Create a new role. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleCreateRequest {
    @NotBlank @Size(max = 50) private String name;
    @NotBlank @Size(max = 100) private String displayName;
    @Size(max = 500) private String description;
}

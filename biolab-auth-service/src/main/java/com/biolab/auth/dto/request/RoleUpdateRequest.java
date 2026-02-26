package com.biolab.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/** Update role details. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleUpdateRequest {
    @Size(max = 100) private String displayName;
    @Size(max = 500) private String description;
}

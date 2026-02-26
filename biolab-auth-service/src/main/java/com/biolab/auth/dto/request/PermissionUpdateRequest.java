package com.biolab.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/** Update permission details. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionUpdateRequest {
    @Size(max = 500) private String description;
    @Size(max = 50) private String module;
}

package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Create a new permission. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionCreateRequest {
    @NotBlank @Size(max = 100) private String name;
    @NotBlank @Size(max = 50) private String module;
    @NotBlank @Size(max = 50) private String action;
    @Size(max = 500) private String description;
}

package com.biolab.auth.dto.response;

import lombok.*;
import java.util.UUID;

/** Permission details response. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionResponse {
    private UUID id;
    private String name;
    private String module;
    private String action;
    private String description;
}

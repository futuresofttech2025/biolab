package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

/** Log a PHI/PII data access event. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DataAccessLogRequest {
    @NotBlank @Size(max = 50) private String resourceType;
    @NotNull private UUID resourceId;
    @NotBlank @Pattern(regexp = "^(VIEW|DOWNLOAD|EXPORT|PRINT|CREATE|UPDATE|DELETE)$") private String action;
}

package com.biolab.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Grant or update consent. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsentRequest {
    @NotBlank @Pattern(regexp = "^(GDPR|HIPAA|TOS|MARKETING)$") private String consentType;
    @Size(max = 20) private String version;
}

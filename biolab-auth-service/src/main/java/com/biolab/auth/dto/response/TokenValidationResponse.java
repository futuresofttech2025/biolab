package com.biolab.auth.dto.response;

import lombok.*;
import java.util.List;

/** Token validation response — used by API Gateway. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TokenValidationResponse {
    private boolean valid;
    private String userId;
    private String email;
    private List<String> roles;
    private String orgId;
}

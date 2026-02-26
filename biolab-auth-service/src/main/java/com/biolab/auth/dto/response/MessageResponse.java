package com.biolab.auth.dto.response;

import lombok.*;
import java.util.Map;

/** Simple message response for confirmations with optional details. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageResponse {
    private String message;
    private String expiresIn;
    private Map<String, Object> details;
}

package com.biolab.auth.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Registration response — new user confirmation. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isEmailVerified;
    private Instant createdAt;
}

package com.biolab.user.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * User-organization membership response.
 *
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserOrganizationResponse {

    private UUID id;
    private UUID userId;
    private UUID organizationId;
    private String organizationName;
    private String organizationType;
    private String roleInOrg;
    private Boolean isPrimary;
    private Instant joinedAt;
}

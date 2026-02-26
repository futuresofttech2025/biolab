package com.biolab.user.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

/**
 * Request to add a user to an organization.
 *
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserOrganizationAssignRequest {

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @Size(max = 50, message = "Role in org must not exceed 50 characters")
    private String roleInOrg;

    private Boolean isPrimary;
}

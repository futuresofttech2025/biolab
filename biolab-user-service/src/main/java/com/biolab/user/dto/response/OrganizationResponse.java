package com.biolab.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Organization details response.
 *
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationResponse {

    private UUID id;
    private String name;
    private String type;
    private String address;
    private String phone;
    private String website;
    private String logoUrl;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    /** Number of members in this organization. */
    private Long memberCount;
}

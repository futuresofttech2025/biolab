package com.biolab.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Organization update request — partial update, only non-null fields applied.
 *
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizationUpdateRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 512, message = "Logo URL must not exceed 512 characters")
    private String logoUrl;

    private Boolean isActive;
}

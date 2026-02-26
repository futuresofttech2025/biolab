package com.biolab.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Organization creation request with validation.
 *
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizationCreateRequest {

    @NotBlank(message = "Organization name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Organization type is required")
    @Pattern(regexp = "^(BUYER|SUPPLIER)$", message = "Type must be BUYER or SUPPLIER")
    private String type;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 512, message = "Logo URL must not exceed 512 characters")
    private String logoUrl;
}

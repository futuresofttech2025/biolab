package com.biolab.user.service;

import com.biolab.user.dto.request.OrganizationCreateRequest;
import com.biolab.user.dto.request.OrganizationUpdateRequest;
import com.biolab.user.dto.response.OrganizationResponse;
import com.biolab.user.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service for organization CRUD — Buyer and Supplier organizations.
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
public interface OrganizationService {

    /** Creates a new organization (BUYER or SUPPLIER). */
    OrganizationResponse create(OrganizationCreateRequest request);

    /** Retrieves an organization by UUID. */
    OrganizationResponse getById(UUID id);

    /** Searches organizations with keyword, type filter, and active filter. Paginated. */
    PageResponse<OrganizationResponse> search(String search, String type, Boolean isActive, Pageable pageable);

    /** Updates organization details (partial update). */
    OrganizationResponse update(UUID id, OrganizationUpdateRequest request);

    /** Soft-deletes an organization. */
    void deactivate(UUID id);
}

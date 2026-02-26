package com.biolab.user.service;

import com.biolab.user.dto.request.UserOrganizationAssignRequest;
import com.biolab.user.dto.response.UserOrganizationResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service for user-organization membership management.
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
public interface UserOrganizationService {

    /** Adds a user to an organization with a role. */
    UserOrganizationResponse addMember(UUID userId, UserOrganizationAssignRequest request);

    /** Lists all organizations a user belongs to. */
    List<UserOrganizationResponse> getByUserId(UUID userId);

    /** Lists all members of an organization. */
    List<UserOrganizationResponse> getByOrganizationId(UUID orgId);

    /** Updates a user's role or primary flag within an organization. */
    UserOrganizationResponse updateMembership(UUID userId, UUID orgId, String roleInOrg, Boolean isPrimary);

    /** Removes a user from an organization. */
    void removeMember(UUID userId, UUID orgId);
}

package com.biolab.user.service.impl;

import com.biolab.user.dto.request.UserOrganizationAssignRequest;
import com.biolab.user.dto.response.UserOrganizationResponse;
import com.biolab.user.entity.Organization;
import com.biolab.user.entity.UserOrganization;
import com.biolab.user.exception.DuplicateResourceException;
import com.biolab.user.exception.ResourceNotFoundException;
import com.biolab.user.repository.OrganizationRepository;
import com.biolab.user.repository.UserOrganizationRepository;
import com.biolab.user.repository.UserRepository;
import com.biolab.user.service.UserOrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link UserOrganizationService} — user-org membership CRUD.
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserOrganizationServiceImpl implements UserOrganizationService {

    private final UserOrganizationRepository userOrgRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public UserOrganizationResponse addMember(UUID userId, UserOrganizationAssignRequest request) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        // Verify organization exists
        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", request.getOrganizationId()));

        // Check for duplicate
        if (userOrgRepository.existsByUserIdAndOrganizationId(userId, request.getOrganizationId())) {
            throw new DuplicateResourceException("Membership", "userId+orgId", userId + "/" + request.getOrganizationId());
        }

        UserOrganization uo = UserOrganization.builder()
                .userId(userId)
                .organization(org)
                .roleInOrg(request.getRoleInOrg() != null ? request.getRoleInOrg() : "MEMBER")
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .build();

        UserOrganization saved = userOrgRepository.save(uo);
        log.info("User {} added to organization {} as {}", userId, org.getName(), uo.getRoleInOrg());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOrganizationResponse> getByUserId(UUID userId) {
        return userOrgRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOrganizationResponse> getByOrganizationId(UUID orgId) {
        return userOrgRepository.findByOrganizationId(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UserOrganizationResponse updateMembership(UUID userId, UUID orgId, String roleInOrg, Boolean isPrimary) {
        UserOrganization uo = userOrgRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership", "userId+orgId", userId + "/" + orgId));

        if (roleInOrg != null) uo.setRoleInOrg(roleInOrg);
        if (isPrimary != null) uo.setIsPrimary(isPrimary);

        UserOrganization saved = userOrgRepository.save(uo);
        log.info("Membership updated: user={}, org={}", userId, orgId);
        return toResponse(saved);
    }

    @Override
    public void removeMember(UUID userId, UUID orgId) {
        if (!userOrgRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new ResourceNotFoundException("Membership", "userId+orgId", userId + "/" + orgId);
        }
        userOrgRepository.deleteByUserIdAndOrganizationId(userId, orgId);
        log.info("User {} removed from organization {}", userId, orgId);
    }

    private UserOrganizationResponse toResponse(UserOrganization uo) {
        return UserOrganizationResponse.builder()
                .id(uo.getId())
                .userId(uo.getUserId())
                .organizationId(uo.getOrganization().getId())
                .organizationName(uo.getOrganization().getName())
                .organizationType(uo.getOrganization().getType().name())
                .roleInOrg(uo.getRoleInOrg())
                .isPrimary(uo.getIsPrimary())
                .joinedAt(uo.getJoinedAt())
                .build();
    }
}

package com.biolab.user.service.impl;

import com.biolab.user.dto.request.UserUpdateRequest;
import com.biolab.user.dto.response.*;
import com.biolab.user.entity.User;
import com.biolab.user.entity.UserOrganization;
import com.biolab.user.exception.ResourceNotFoundException;
import com.biolab.user.repository.UserOrganizationRepository;
import com.biolab.user.repository.UserRepository;
import com.biolab.user.repository.UserRoleRepository;
import com.biolab.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link UserProfileService} — manages user profile CRUD.
 *
 * <p>This service reads from both {@code sec_schema.users} and joins with
 * user_roles and user_organizations to build complete user profiles.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserOrganizationRepository userOrgRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return buildFullProfile(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return buildFullProfile(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> search(String search, Boolean isActive, Pageable pageable) {
        Page<User> page = userRepository.searchUsers(search, isActive, pageable);
        List<UserProfileResponse> content = page.getContent().stream()
                .map(this::buildFullProfile)
                .toList();

        return PageResponse.<UserProfileResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Override
    public UserProfileResponse update(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Apply partial update — only non-null fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        user.setUpdatedAt(Instant.now());

        User saved = userRepository.save(user);
        log.info("User profile updated: {}", id);
        return buildFullProfile(saved);
    }

    @Override
    public void deactivate(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        log.info("User deactivated (soft delete): {}", id);
    }

    @Override
    public UserProfileResponse reactivate(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(true);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("User reactivated: {}", id);
        return buildFullProfile(saved);
    }

    // ─── Helper: Build full profile with roles and orgs ─────────────

    /**
     * Builds a complete user profile response with roles and organization memberships.
     *
     * @param user the User entity
     * @return the fully populated UserProfileResponse
     */
    private UserProfileResponse buildFullProfile(User user) {
        // Fetch role names
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());

        // Fetch organization memberships
        List<UserOrganization> orgs = userOrgRepository.findByUserId(user.getId());
        List<UserOrganizationResponse> orgResponses = orgs.stream()
                .map(uo -> UserOrganizationResponse.builder()
                        .id(uo.getId())
                        .userId(uo.getUserId())
                        .organizationId(uo.getOrganization().getId())
                        .organizationName(uo.getOrganization().getName())
                        .organizationType(uo.getOrganization().getType().name())
                        .roleInOrg(uo.getRoleInOrg())
                        .isPrimary(uo.getIsPrimary())
                        .joinedAt(uo.getJoinedAt())
                        .build())
                .toList();

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .isLocked(user.getIsLocked())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(roles)
                .organizations(orgResponses)
                .build();
    }
}

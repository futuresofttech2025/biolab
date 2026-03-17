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
 * Implementation of {@link UserProfileService}.
 *
 * <h3>Sprint 2/3 — Email encryption changes</h3>
 * <p>{@code user.getEmail()} now returns the HMAC-SHA256 hash (not displayable).
 * All response objects must use {@code user.getEmailDisplay()} for the
 * human-readable email address. The hash is only used for DB lookups.</p>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository              userRepository;
    private final UserRoleRepository          userRoleRepository;
    private final UserOrganizationRepository  userOrgRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return buildFullProfile(user);
    }

    /**
     * Sprint 2/3: lookup is by email hash — the converter handles the hashing.
     * Response uses emailDisplay for the human-readable address.
     */
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
                .content(content).page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .hasNext(page.hasNext()).build();
    }

    @Override
    public UserProfileResponse update(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        if (request.getFirstName()  != null) user.setFirstName(request.getFirstName().trim());
        if (request.getLastName()   != null) user.setLastName(request.getLastName().trim());
        if (request.getPhone()      != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl()  != null) user.setAvatarUrl(request.getAvatarUrl());
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
        log.info("User deactivated: {}", id);
    }

    @Override
    public UserProfileResponse reactivate(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(true);
        user.setUpdatedAt(Instant.now());
        return buildFullProfile(userRepository.save(user));
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private UserProfileResponse buildFullProfile(User user) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        List<UserOrganization> orgs = userOrgRepository.findByUserId(user.getId());

        List<UserOrganizationResponse> orgResponses = orgs.stream()
                .map(uo -> UserOrganizationResponse.builder()
                        .id(uo.getId()).userId(uo.getUserId())
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
                // Sprint 2/3: use emailDisplay (decrypted AES) for display,
                // NOT user.getEmail() which is the HMAC hash.
                .email(user.getEmailDisplay() != null
                        ? user.getEmailDisplay()
                        : user.getEmail()) // fallback for pre-migration records
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
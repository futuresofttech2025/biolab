package com.biolab.user.service;

import com.biolab.user.dto.request.UserUpdateRequest;
import com.biolab.user.dto.response.PageResponse;
import com.biolab.user.dto.response.UserProfileResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service for user profile management — CRUD operations on user profiles.
 *
 * <p>Note: User creation and password management are handled by the Auth Service.
 * This service manages profile-level reads and updates.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
public interface UserProfileService {

    /** Retrieves a full user profile by UUID — includes roles and organizations. */
    UserProfileResponse getById(UUID id);

    /** Retrieves a user profile by email address. */
    UserProfileResponse getByEmail(String email);

    /** Searches users by keyword (email/name) with optional active filter. Paginated. */
    PageResponse<UserProfileResponse> search(String search, Boolean isActive, Pageable pageable);

    /** Updates user profile fields (partial update — only non-null fields applied). */
    UserProfileResponse update(UUID id, UserUpdateRequest request);

    /** Soft-deletes a user by setting is_active=false. */
    void deactivate(UUID id);

    /** Reactivates a previously deactivated user. */
    UserProfileResponse reactivate(UUID id);
}

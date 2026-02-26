package com.biolab.auth.service;

import com.biolab.auth.dto.request.UserCreateRequest;
import com.biolab.auth.dto.request.UserUpdateRequest;
import com.biolab.auth.dto.response.PageResponse;
import com.biolab.auth.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/** CRUD operations for users table. */
public interface UserCrudService {
    UserResponse create(UserCreateRequest request);
    UserResponse getById(UUID id);
    UserResponse getByEmail(String email);
    PageResponse<UserResponse> search(String search, Boolean isActive, Pageable pageable);
    UserResponse update(UUID id, UserUpdateRequest request);
    void delete(UUID id);
}

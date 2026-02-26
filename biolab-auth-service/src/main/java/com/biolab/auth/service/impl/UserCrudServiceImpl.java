package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.*;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.User;
import com.biolab.auth.exception.*;
import com.biolab.auth.repository.UserRepository;
import com.biolab.auth.service.UserCrudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

/** CRUD implementation for sec_schema.users. */
@Service @RequiredArgsConstructor @Slf4j @Transactional
public class UserCrudServiceImpl implements UserCrudService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse create(UserCreateRequest req) {
        if (repo.existsByEmailIgnoreCase(req.getEmail()))
            throw new DuplicateResourceException("User", "email", req.getEmail());
        User u = User.builder().email(req.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName()).lastName(req.getLastName()).phone(req.getPhone())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .isEmailVerified(req.getIsEmailVerified() != null ? req.getIsEmailVerified() : false)
                .passwordChangedAt(Instant.now()).build();
        return toResponse(repo.save(u));
    }

    @Override @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return toResponse(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id)));
    }

    @Override @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return toResponse(repo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User","email",email)));
    }

    @Override @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String search, Boolean isActive, Pageable pageable) {
        var page = repo.searchUsers(search, isActive, pageable);
        return PageResponse.<UserResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .hasNext(page.hasNext()).build();
    }

    @Override
    public UserResponse update(UUID id, UserUpdateRequest req) {
        User u = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id));
        if (req.getFirstName() != null) u.setFirstName(req.getFirstName());
        if (req.getLastName() != null) u.setLastName(req.getLastName());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getAvatarUrl() != null) u.setAvatarUrl(req.getAvatarUrl());
        if (req.getIsActive() != null) u.setIsActive(req.getIsActive());
        if (req.getIsEmailVerified() != null) u.setIsEmailVerified(req.getIsEmailVerified());
        u.setUpdatedAt(Instant.now());
        return toResponse(repo.save(u));
    }

    @Override
    public void delete(UUID id) {
        User u = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id));
        u.setIsActive(false); u.setUpdatedAt(Instant.now()); repo.save(u);
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder().id(u.getId()).email(u.getEmail())
                .firstName(u.getFirstName()).lastName(u.getLastName())
                .phone(u.getPhone()).avatarUrl(u.getAvatarUrl())
                .isActive(u.getIsActive()).isEmailVerified(u.getIsEmailVerified())
                .isLocked(u.getIsLocked()).lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt()).updatedAt(u.getUpdatedAt()).build();
    }
}

package com.biolab.user.service.impl;

import com.biolab.user.dto.response.RoleResponse;
import com.biolab.user.dto.response.UserRoleResponse;
import com.biolab.user.entity.Role;
import com.biolab.user.exception.ResourceNotFoundException;
import com.biolab.user.repository.RolePermissionRepository;
import com.biolab.user.repository.RoleRepository;
import com.biolab.user.repository.UserRoleRepository;
import com.biolab.user.service.UserRoleQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read-only service for querying user roles and role details.
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserRoleQueryServiceImpl implements UserRoleQueryService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public List<UserRoleResponse> getRolesByUserId(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(ur -> UserRoleResponse.builder()
                        .id(ur.getId())
                        .userId(ur.getUser().getId())
                        .roleName(ur.getRole().getName())
                        .roleDisplayName(ur.getRole().getDisplayName())
                        .assignedBy(ur.getAssignedBy())
                        .assignedAt(ur.getAssignedAt())
                        .expiresAt(ur.getExpiresAt())
                        .build())
                .toList();
    }

    @Override
    public List<String> getRoleNamesByUserId(UUID userId) {
        return userRoleRepository.findRoleNamesByUserId(userId);
    }

    @Override
    public RoleResponse getRoleById(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        List<String> permissions = rolePermissionRepository.findPermissionNamesByRoleId(roleId);

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .isSystemRole(role.getIsSystemRole())
                .createdAt(role.getCreatedAt())
                .permissions(permissions)
                .build();
    }

    @Override
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .displayName(role.getDisplayName())
                        .description(role.getDescription())
                        .isSystemRole(role.getIsSystemRole())
                        .createdAt(role.getCreatedAt())
                        .permissions(rolePermissionRepository.findPermissionNamesByRoleId(role.getId()))
                        .build())
                .toList();
    }

    @Override
    public boolean hasRole(UUID userId, String roleName) {
        return userRoleRepository.findRoleNamesByUserId(userId).contains(roleName);
    }
}

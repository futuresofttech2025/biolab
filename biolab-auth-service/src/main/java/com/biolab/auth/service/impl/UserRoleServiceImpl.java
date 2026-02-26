package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.UserRoleAssignRequest;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.*;
import com.biolab.auth.exception.*;
import com.biolab.auth.repository.*;
import com.biolab.auth.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j @Transactional
public class UserRoleServiceImpl implements UserRoleService {
    private final UserRoleRepository repo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;

    @Override
    public UserRoleResponse assign(UUID userId, UserRoleAssignRequest req, UUID assignedBy) {
        User user = userRepo.findById(userId).orElseThrow(()->new ResourceNotFoundException("User","id",userId));
        Role role = roleRepo.findById(req.getRoleId()).orElseThrow(()->new ResourceNotFoundException("Role","id",req.getRoleId()));
        if (repo.existsByUserIdAndRoleId(userId, req.getRoleId()))
            throw new DuplicateResourceException("UserRole","userId+roleId",userId+"/"+req.getRoleId());
        UserRole ur = UserRole.builder().user(user).role(role).assignedBy(assignedBy).expiresAt(req.getExpiresAt()).build();
        UserRole saved = repo.save(ur);
        return toResp(saved);
    }

    @Override @Transactional(readOnly=true)
    public List<UserRoleResponse> getByUserId(UUID userId) { return repo.findByUserId(userId).stream().map(this::toResp).toList(); }

    @Override @Transactional(readOnly=true)
    public List<UserRoleResponse> getByRoleId(UUID roleId) { return repo.findByRoleId(roleId).stream().map(this::toResp).toList(); }

    @Override
    public void revoke(UUID userId, UUID roleId) {
        if (!repo.existsByUserIdAndRoleId(userId, roleId))
            throw new ResourceNotFoundException("UserRole","userId+roleId",userId+"/"+roleId);
        repo.deleteByUserIdAndRoleId(userId, roleId);
    }

    private UserRoleResponse toResp(UserRole ur) {
        return UserRoleResponse.builder().id(ur.getId()).userId(ur.getUser().getId())
                .role(RoleResponse.builder().id(ur.getRole().getId()).name(ur.getRole().getName())
                        .displayName(ur.getRole().getDisplayName()).build())
                .assignedBy(ur.getAssignedBy()).assignedAt(ur.getAssignedAt()).expiresAt(ur.getExpiresAt()).build();
    }
}

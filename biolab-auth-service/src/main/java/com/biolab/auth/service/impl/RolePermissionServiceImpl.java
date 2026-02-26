package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.RolePermissionAssignRequest;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.*;
import com.biolab.auth.exception.*;
import com.biolab.auth.repository.*;
import com.biolab.auth.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j @Transactional
public class RolePermissionServiceImpl implements RolePermissionService {
    private final RolePermissionRepository repo;
    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;

    @Override
    public RolePermissionResponse assignPermissions(UUID roleId, RolePermissionAssignRequest req) {
        Role role = roleRepo.findById(roleId).orElseThrow(()->new ResourceNotFoundException("Role","id",roleId));
        for (UUID permId : req.getPermissionIds()) {
            if (!repo.existsByRoleIdAndPermissionId(roleId, permId)) {
                Permission perm = permRepo.findById(permId).orElseThrow(()->new ResourceNotFoundException("Permission","id",permId));
                repo.save(RolePermission.builder().role(role).permission(perm).build());
            }
        }
        return getByRoleId(roleId);
    }

    @Override @Transactional(readOnly=true)
    public RolePermissionResponse getByRoleId(UUID roleId) {
        Role role = roleRepo.findById(roleId).orElseThrow(()->new ResourceNotFoundException("Role","id",roleId));
        var perms = repo.findByRoleId(roleId).stream()
                .map(rp -> PermissionResponse.builder().id(rp.getPermission().getId())
                        .name(rp.getPermission().getName()).module(rp.getPermission().getModule())
                        .action(rp.getPermission().getAction()).description(rp.getPermission().getDescription()).build())
                .toList();
        return RolePermissionResponse.builder().roleId(roleId).roleName(role.getName()).permissions(perms).build();
    }

    @Override
    public void revokePermission(UUID roleId, UUID permissionId) {
        if (!repo.existsByRoleIdAndPermissionId(roleId, permissionId))
            throw new ResourceNotFoundException("RolePermission","roleId+permId",roleId+"/"+permissionId);
        repo.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }
}

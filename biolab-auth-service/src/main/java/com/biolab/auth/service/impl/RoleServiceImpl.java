package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.*;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.*;
import com.biolab.auth.exception.*;
import com.biolab.auth.repository.*;
import com.biolab.auth.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** CRUD implementation for Role. */
@Service @RequiredArgsConstructor @Slf4j @Transactional
public class RoleServiceImpl implements RoleService {
    private final RoleRepository repo;

    @Override public RoleResponse create(RoleCreateRequest req) {
        if (repo.existsByName(req.getName())) throw new DuplicateResourceException("Role","name",req.getName());
        Role r = Role.builder().name(req.getName().toUpperCase()).displayName(req.getDisplayName())
                .description(req.getDescription()).build();
        return toResp(repo.save(r));
    }
    @Override @Transactional(readOnly=true) public List<RoleResponse> getAll() { return repo.findAll().stream().map(this::toResp).toList(); }
    @Override @Transactional(readOnly=true) public RoleResponse getById(UUID id) { return toResp(repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Role","id",id))); }
    @Override @Transactional(readOnly=true) public RoleResponse getByName(String name) { return toResp(repo.findByName(name).orElseThrow(()->new ResourceNotFoundException("Role","name",name))); }
    @Override public RoleResponse update(UUID id, RoleUpdateRequest req) {
        Role r = repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Role","id",id));
        if (req.getDisplayName()!=null) r.setDisplayName(req.getDisplayName());
        if (req.getDescription()!=null) r.setDescription(req.getDescription());
        r.setUpdatedAt(Instant.now()); return toResp(repo.save(r));
    }
    @Override public void delete(UUID id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Role","id",id);
        repo.deleteById(id);
    }
    private RoleResponse toResp(Role r) {
        return RoleResponse.builder().id(r.getId()).name(r.getName()).displayName(r.getDisplayName())
                .description(r.getDescription()).isSystemRole(r.getIsSystemRole()).createdAt(r.getCreatedAt()).build();
    }
}

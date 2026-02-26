package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.*;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.*;
import com.biolab.auth.exception.*;
import com.biolab.auth.repository.*;
import com.biolab.auth.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** CRUD implementation for Permission. */
@Service @RequiredArgsConstructor @Slf4j @Transactional
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository repo;

    @Override public PermissionResponse create(PermissionCreateRequest req) {
        if (repo.existsByName(req.getName())) throw new DuplicateResourceException("Permission","name",req.getName());
        Permission p = Permission.builder().name(req.getName()).module(req.getModule())
                .action(req.getAction()).description(req.getDescription()).build();
        return toResp(repo.save(p));
    }
    @Override @Transactional(readOnly=true) public List<PermissionResponse> getAll() { return repo.findAll().stream().map(this::toResp).toList(); }
    @Override @Transactional(readOnly=true) public PermissionResponse getById(UUID id) { return toResp(repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Permission","id",id))); }
    @Override @Transactional(readOnly=true) public List<PermissionResponse> getByModule(String module) { return repo.findByModule(module).stream().map(this::toResp).toList(); }
    @Override public PermissionResponse update(UUID id, PermissionUpdateRequest req) {
        Permission p = repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Permission","id",id));
        if (req.getDescription()!=null) p.setDescription(req.getDescription());
        if (req.getModule()!=null) p.setModule(req.getModule());
        p.setUpdatedAt(Instant.now()); return toResp(repo.save(p));
    }
    @Override public void delete(UUID id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Permission","id",id);
        repo.deleteById(id);
    }
    private PermissionResponse toResp(Permission p) {
        return PermissionResponse.builder().id(p.getId()).name(p.getName()).module(p.getModule())
                .action(p.getAction()).description(p.getDescription()).build();
    }
}
